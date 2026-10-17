package com.example.lev.data.remote

import com.example.lev.domain.SheetRow
import com.example.lev.domain.SheetTab
import com.example.lev.domain.SyncErrorKind
import com.example.lev.domain.SyncException
import com.example.lev.domain.SyncProtocol
import com.example.lev.domain.SyncResponse
import com.example.lev.domain.actionableMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.io.InterruptedIOException
import java.util.concurrent.TimeUnit

/**
 * Cliente HTTP do sync (M7 §B/§F): OkHttp + Moshi (via [SyncProtocol]).
 *
 * - POST JSON na URL /exec; o OkHttp segue o redirect 302 do Apps Script e lê
 *   o JSON do destino sem reenviar o POST.
 * - Timeout de 15 s, retry exponencial (3 tentativas) em falhas de rede, 5xx e
 *   HTML de erro do Google. Reenvios repetem o mesmo `requestId` (idempotente).
 * - Erros acionáveis em pt-BR (401/403, 404, script antigo, cota, sem acesso).
 */
interface SheetsGateway {
    suspend fun ping(
        url: String,
        secret: String,
        deviceId: String,
    ): SyncResponse

    suspend fun setup(
        url: String,
        secret: String,
        deviceId: String,
    ): SyncResponse

    suspend fun push(
        url: String,
        secret: String,
        deviceId: String,
        tab: SheetTab,
        rows: List<SheetRow>,
        replace: Boolean,
        requestId: String = SyncProtocol.newRequestId(),
    ): SyncResponse

    suspend fun pull(
        url: String,
        secret: String,
        deviceId: String,
        tab: SheetTab,
        since: Long,
        offset: Int,
        limit: Int,
    ): SyncResponse

    suspend fun backup(
        url: String,
        secret: String,
        deviceId: String,
    ): SyncResponse

    suspend fun compact(
        url: String,
        secret: String,
        deviceId: String,
        olderThanMs: Long,
    ): SyncResponse
}

class SheetsClient(
    private val http: OkHttpClient = defaultHttpClient(),
    private val maxRetries: Int = SyncProtocol.MAX_RETRIES,
) : SheetsGateway {
    override suspend fun ping(
        url: String,
        secret: String,
        deviceId: String,
    ): SyncResponse = call(url, SyncProtocol.Action.PING, secret, deviceId, emptyMap(), SyncProtocol.newRequestId())

    override suspend fun setup(
        url: String,
        secret: String,
        deviceId: String,
    ): SyncResponse = call(url, SyncProtocol.Action.SETUP, secret, deviceId, emptyMap(), SyncProtocol.newRequestId())

    override suspend fun push(
        url: String,
        secret: String,
        deviceId: String,
        tab: SheetTab,
        rows: List<SheetRow>,
        replace: Boolean,
        requestId: String,
    ): SyncResponse {
        // null → "" : célula vazia na planilha.
        val sanitized = rows.map { row -> row.mapValues { (_, v) -> v ?: "" } }
        return call(
            url = url,
            action = SyncProtocol.Action.PUSH,
            secret = secret,
            deviceId = deviceId,
            payload = mapOf("tab" to tab.key, "rows" to sanitized, "replace" to replace),
            requestId = requestId,
        )
    }

    override suspend fun pull(
        url: String,
        secret: String,
        deviceId: String,
        tab: SheetTab,
        since: Long,
        offset: Int,
        limit: Int,
    ): SyncResponse =
        call(
            url = url,
            action = SyncProtocol.Action.PULL,
            secret = secret,
            deviceId = deviceId,
            payload = mapOf("tab" to tab.key, "since" to since, "offset" to offset, "limit" to limit),
            requestId = SyncProtocol.newRequestId(),
        )

    override suspend fun backup(
        url: String,
        secret: String,
        deviceId: String,
    ): SyncResponse = call(url, SyncProtocol.Action.BACKUP, secret, deviceId, emptyMap(), SyncProtocol.newRequestId())

    override suspend fun compact(
        url: String,
        secret: String,
        deviceId: String,
        olderThanMs: Long,
    ): SyncResponse =
        call(
            url = url,
            action = SyncProtocol.Action.COMPACT,
            secret = secret,
            deviceId = deviceId,
            payload = mapOf("olderThan" to olderThanMs),
            requestId = SyncProtocol.newRequestId(),
        )

    private suspend fun call(
        url: String,
        action: String,
        secret: String,
        deviceId: String,
        payload: Map<String, Any?>,
        requestId: String,
    ): SyncResponse {
        val envelope = SyncProtocol.buildEnvelope(action, secret, deviceId, payload, requestId = requestId)
        val body = SyncProtocol.toJson(envelope).toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder().url(normalizeUrl(url)).post(body).build()

        var attempt = 0
        var lastError: IOException? = null
        var lastElapsedMs = 0L
        while (attempt < maxRetries) {
            val attemptStart = System.currentTimeMillis()
            try {
                return executeOnce(request)
            } catch (e: SyncException) {
                throw e
            } catch (e: IOException) {
                lastError = e
                lastElapsedMs = System.currentTimeMillis() - attemptStart
            }
            attempt++
            if (attempt < maxRetries) delay(RETRY_BASE_MS shl attempt)
        }
        val cause = lastError ?: IOException("falha desconhecida")
        throw mapFinalError(cause, lastElapsedMs)
    }

    private fun mapFinalError(
        cause: IOException,
        elapsedMs: Long,
    ): SyncException {
        if (isTimeout(cause, elapsedMs, http.callTimeoutMillis)) {
            return SyncException(SyncErrorKind.TIMEOUT, SyncErrorKind.TIMEOUT.actionableMessage())
        }
        if (cause is RetryableException) return SyncException(cause.kind, cause.message ?: "")
        // Inclui a classe da exceção para diagnóstico (sem segredos): ex. UnknownHostException.
        val detail = cause.javaClass.simpleName
        return SyncException(
            SyncErrorKind.NETWORK,
            SyncErrorKind.NETWORK.actionableMessage() + " [$detail]",
        )
    }

    /** Uma tentativa: sucesso, falha fatal ou falha retryable (M7 §B). */
    private suspend fun executeOnce(request: Request): SyncResponse =
        withContext(Dispatchers.IO) {
            http.newCall(request).execute().use { response ->
                val rawBody = runCatching { response.body?.string() }.getOrNull()
                val failure = classifyFailure(response.code, response.isSuccessful, rawBody)
                if (failure != null) throw failure.toException()
                SyncProtocol.parseResponse(rawBody ?: "")
            }
        }

    private fun classifyFailure(
        httpCode: Int,
        successful: Boolean,
        body: String?,
    ): HttpFailure? {
        if (!successful) {
            val kind = SyncProtocol.classifyHttpError(httpCode, body)
            // 5xx e HTML: tenta de novo; 401/403/404: falha direto (acionável).
            return HttpFailure(kind, retryable = kind == SyncErrorKind.SERVER || kind == SyncErrorKind.HTML_ERROR)
        }
        if ((body ?: "").trimStart().startsWith("<", ignoreCase = true)) {
            return HttpFailure(SyncErrorKind.HTML_ERROR, retryable = true)
        }
        return null
    }

    private fun normalizeUrl(url: String): String {
        // Cola de mensageiros costuma trazer aspas/crases junto: remove.
        val trimmed = url.trim().trim('\'', '"', '`', '‘', '’', '“', '”')
        require(trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            "URL inválida: use a URL /exec da implantação."
        }
        return trimmed
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private const val RETRY_BASE_MS = 1000L

        fun defaultHttpClient(): OkHttpClient =
            OkHttpClient.Builder()
                .callTimeout(SyncProtocol.CALL_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .retryOnConnectionFailure(true)
                .build()

        /** Timeouts do OkHttp: tipo/mensagem ou falha colada no deadline do callTimeout. */
        internal fun isTimeout(
            e: IOException,
            elapsedMs: Long = 0L,
            deadlineMs: Int = 0,
        ): Boolean =
            e is InterruptedIOException || e.message?.contains("timeout", ignoreCase = true) == true ||
                (deadlineMs > 0 && elapsedMs >= deadlineMs - TIMEOUT_GRACE_MS)

        private const val TIMEOUT_GRACE_MS = 250
    }
}

private class RetryableException(val kind: SyncErrorKind, message: String) : IOException(message)

private data class HttpFailure(val kind: SyncErrorKind, val retryable: Boolean) {
    fun toException(): Exception =
        if (retryable) {
            RetryableException(kind, kind.actionableMessage())
        } else {
            SyncException(kind, kind.actionableMessage())
        }
}
