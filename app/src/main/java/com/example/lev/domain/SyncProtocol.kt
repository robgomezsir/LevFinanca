package com.example.lev.domain

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Protocolo do sync com Google Planilhas via Apps Script (M7 §B).
 *
 * POST JSON na URL /exec com campos comuns: action, requestId (UUID, reenvios
 * idempotentes), ts (epoch ms), sig (HMAC-SHA256 de "ts.action.requestId" com o
 * segredo), schemaVersion e deviceId. Lotes de até 500 linhas por requisição e
 * por aba. Respostas sempre JSON: {status, message, counts, serverTime,
 * scriptVersion}. O Apps Script responde com redirect 302: o OkHttp segue o
 * redirect e lê o JSON do destino, sem reenviar o POST.
 */
object SyncProtocol {
    const val PROTOCOL_VERSION = 1
    const val EXPECTED_SCRIPT_VERSION = "1.0.0"
    const val MAX_BATCH_ROWS = 500
    const val TS_WINDOW_MS = 5L * 60L * 1000L
    const val CALL_TIMEOUT_MS = 15_000
    const val MAX_RETRIES = 3

    object Action {
        const val PING = "ping"
        const val SETUP = "setup"
        const val PUSH = "sync_push"
        const val PULL = "sync_pull"
        const val BACKUP = "backup"
        const val COMPACT = "compact"
    }

    private val moshi: Moshi = Moshi.Builder().build()
    private val mapAdapter =
        moshi.adapter<Map<String, Any?>>(
            Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java),
        )

    fun newRequestId(): String = UUID.randomUUID().toString()

    /** HMAC-SHA256 hex de "ts.action.requestId" com o segredo. */
    fun sign(
        secret: String,
        ts: Long,
        action: String,
        requestId: String,
    ): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val payload = "$ts.$action.$requestId"
        val bytes = mac.doFinal(payload.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun isTimestampFresh(
        ts: Long,
        nowMs: Long,
        windowMs: Long = TS_WINDOW_MS,
    ): Boolean = ts in (nowMs - windowMs)..(nowMs + windowMs)

    fun buildEnvelope(
        action: String,
        secret: String,
        deviceId: String,
        payload: Map<String, Any?> = emptyMap(),
        ts: Long = System.currentTimeMillis(),
        requestId: String = newRequestId(),
    ): Map<String, Any?> {
        val envelope =
            mutableMapOf<String, Any?>(
                "action" to action,
                "requestId" to requestId,
                "ts" to ts,
                "sig" to sign(secret, ts, action, requestId),
                "schemaVersion" to PROTOCOL_VERSION,
                "deviceId" to deviceId,
            )
        envelope.putAll(payload)
        return envelope
    }

    fun toJson(value: Map<String, Any?>): String = mapAdapter.toJson(value)

    @Suppress("UNCHECKED_CAST")
    fun parseResponse(json: String): SyncResponse {
        val map =
            mapAdapter.fromJson(json) ?: throw SyncException(
                SyncErrorKind.PROTOCOL,
                "Resposta vazia do script.",
            )
        checkStatusOk(map)
        checkScriptVersion(map)
        return readOk(map)
    }

    private fun checkStatusOk(map: Map<String, Any?>) {
        val status =
            (map["status"] as? String) ?: throw SyncException(
                SyncErrorKind.PROTOCOL,
                "Resposta sem campo 'status'.",
            )
        if (status != "ok") {
            val code = (map["code"] as? String) ?: "ERROR"
            val message = (map["message"] as? String) ?: "Erro no script."
            throw SyncException(classifyServerCode(code), message)
        }
    }

    private fun checkScriptVersion(map: Map<String, Any?>) {
        val scriptVersion = (map["scriptVersion"] as? String) ?: ""
        if (scriptVersion.isNotBlank() && scriptVersion != EXPECTED_SCRIPT_VERSION) {
            throw SyncException(
                SyncErrorKind.OLD_SCRIPT,
                "Script antigo ($scriptVersion). Reimplante a nova versão " +
                    "(Implantar > Gerenciar implantações > Nova versão) e cole a nova URL /exec.",
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun readOk(map: Map<String, Any?>): SyncResponse {
        val counts =
            (map["counts"] as? Map<String, Any?>)?.mapValues { (_, v) ->
                (v as? Number)?.toLong() ?: v.toString().toLongOrNull() ?: 0L
            }
                ?: emptyMap()
        val rows =
            (map["rows"] as? List<*>)?.mapNotNull { row ->
                (row as? Map<*, *>)?.entries?.associate { (k, v) -> k.toString() to v?.toString() }
            } ?: emptyList()
        return SyncResponse(
            message = (map["message"] as? String) ?: "",
            counts = counts,
            serverTime = (map["serverTime"] as? Number)?.toLong() ?: 0L,
            scriptVersion = (map["scriptVersion"] as? String) ?: "",
            rows = rows,
            hasMore = (map["hasMore"] as? Boolean) ?: false,
            nextOffset = (map["nextOffset"] as? Number)?.toInt() ?: 0,
            total = (map["total"] as? Number)?.toInt() ?: rows.size,
        )
    }

    /** Mapeia o `code` de erro do script para a categoria local. */
    fun classifyServerCode(code: String): SyncErrorKind =
        when (code.uppercase()) {
            "UNAUTHORIZED", "FORBIDDEN", "INVALID_SECRET" -> SyncErrorKind.UNAUTHORIZED
            "NOT_FOUND" -> SyncErrorKind.NOT_FOUND
            "OLD_SCRIPT", "SCHEMA_MISMATCH" -> SyncErrorKind.OLD_SCRIPT
            "QUOTA", "QUOTA_EXCEEDED" -> SyncErrorKind.QUOTA_EXCEEDED
            "SHEET_INACCESSIBLE", "NO_SHEET" -> SyncErrorKind.SHEET_INACCESSIBLE
            else -> SyncErrorKind.SERVER
        }

    /** Erros HTTP/HTML do Google (fora do JSON do script) → categoria + mensagem acionável. */
    fun classifyHttpError(
        httpCode: Int?,
        body: String?,
    ): SyncErrorKind {
        if (httpCode == 401 || httpCode == 403) return SyncErrorKind.UNAUTHORIZED
        if (httpCode == 404) return SyncErrorKind.NOT_FOUND
        if (httpCode != null && httpCode >= 500) return SyncErrorKind.SERVER
        val looksHtml = body?.trimStart()?.startsWith("<", ignoreCase = true) == true
        if (looksHtml) return SyncErrorKind.HTML_ERROR
        return SyncErrorKind.NETWORK
    }
}

data class SyncResponse(
    val message: String,
    val counts: Map<String, Long> = emptyMap(),
    val serverTime: Long = 0L,
    val scriptVersion: String = "",
    val rows: List<Map<String, String?>> = emptyList(),
    val hasMore: Boolean = false,
    val nextOffset: Int = 0,
    val total: Int = 0,
)

enum class SyncErrorKind {
    UNAUTHORIZED,
    NOT_FOUND,
    OLD_SCRIPT,
    QUOTA_EXCEEDED,
    SHEET_INACCESSIBLE,
    NETWORK,
    TIMEOUT,
    SERVER,
    HTML_ERROR,
    PROTOCOL,
    OFFLINE,
    UNKNOWN,
}

class SyncException(
    val kind: SyncErrorKind,
    override val message: String,
) : Exception(message)

/** Mensagens acionáveis em pt-BR (M7 §B, mesmo espírito do Papecon). */
fun SyncErrorKind.actionableMessage(): String =
    when (this) {
        SyncErrorKind.UNAUTHORIZED ->
            "Acesso negado (401/403). Verifique se a implantação está como " +
                "\"Qualquer pessoa\" e se o segredo confere com configurarSegredo()."
        SyncErrorKind.NOT_FOUND ->
            "URL não encontrada (404). Confira se a URL /exec está correta e se a " +
                "implantação não foi removida."
        SyncErrorKind.OLD_SCRIPT ->
            "Script antigo. Reimplante com Nova versão em Implantar > Gerenciar " +
                "implantações e cole a nova URL /exec no app."
        SyncErrorKind.QUOTA_EXCEEDED ->
            "Cota do Apps Script excedida. Aguarde alguns minutos e tente de novo."
        SyncErrorKind.SHEET_INACCESSIBLE ->
            "Planilha inacessível. Confira se ela existe e se o script está vinculado a ela."
        SyncErrorKind.NETWORK ->
            "Sem conexão ou falha de rede. O app continua 100% offline; tente de novo."
        SyncErrorKind.TIMEOUT ->
            "Tempo esgotado (15 s). Tente de novo; reenvios usam o mesmo requestId e são seguros."
        SyncErrorKind.SERVER ->
            "Erro no servidor do Google (5xx). Aguarde e tente de novo."
        SyncErrorKind.HTML_ERROR ->
            "O Google devolveu uma página HTML em vez de JSON. Confira a URL /exec e a implantação."
        SyncErrorKind.PROTOCOL ->
            "Resposta inesperada do script. Atualize o Code.gs para a versão atual."
        SyncErrorKind.OFFLINE ->
            "Sem conexão. O app continua 100% offline; sincronize quando houver rede."
        SyncErrorKind.UNKNOWN ->
            "Erro desconhecido no sync. Tente de novo."
    }
