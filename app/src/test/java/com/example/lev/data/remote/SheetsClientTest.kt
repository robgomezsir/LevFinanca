package com.example.lev.data.remote

import com.example.lev.domain.SheetSchema
import com.example.lev.domain.SyncErrorKind
import com.example.lev.domain.SyncException
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class SheetsClientTest {
    private lateinit var server: MockWebServer
    private lateinit var client: SheetsClient
    private lateinit var url: String

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = SheetsClient()
        url = server.url("/exec").toString()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun okJson(
        message: String = "Pong.",
        extra: String = "",
    ): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(
                """{"status":"ok","message":"$message","counts":{},"serverTime":1,
                   |"scriptVersion":"1.0.0"$extra}
                """.trimMargin(),
            )

    @Test
    fun `ping com sucesso lê mensagem e versão`() {
        server.enqueue(okJson())
        val res = kotlinx.coroutines.runBlocking { client.ping(url, "segredo", "dev-1") }
        assertEquals("Pong.", res.message)
        assertEquals("1.0.0", res.scriptVersion)
    }

    @Test
    fun `script antigo falha com mensagem acionável`() {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"status":"ok","message":"Pong.","scriptVersion":"0.5"}"""),
        )
        try {
            kotlinx.coroutines.runBlocking { client.ping(url, "segredo", "dev-1") }
            fail("deveria lançar")
        } catch (e: SyncException) {
            assertEquals(SyncErrorKind.OLD_SCRIPT, e.kind)
        }
    }

    @Test
    fun `erro do script mapeia código INVALID_SECRET`() {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"status":"error","code":"INVALID_SECRET","message":"Assinatura inválida."}"""),
        )
        try {
            kotlinx.coroutines.runBlocking { client.ping(url, "segredo", "dev-1") }
            fail("deveria lançar")
        } catch (e: SyncException) {
            assertEquals(SyncErrorKind.UNAUTHORIZED, e.kind)
        }
    }

    @Test
    fun `HTTP 401 vira mensagem acionável sem retry`() {
        server.enqueue(MockResponse().setResponseCode(401).setBody("Unauthorized"))
        try {
            kotlinx.coroutines.runBlocking { client.ping(url, "segredo", "dev-1") }
            fail("deveria lançar")
        } catch (e: SyncException) {
            assertEquals(SyncErrorKind.UNAUTHORIZED, e.kind)
            assertTrue(e.message!!.contains("Qualquer pessoa"))
            assertEquals(1, server.requestCount)
        }
    }

    @Test
    fun `HTTP 404 indica URL errada`() {
        server.enqueue(MockResponse().setResponseCode(404).setBody("Not found"))
        try {
            kotlinx.coroutines.runBlocking { client.ping(url, "segredo", "dev-1") }
            fail("deveria lançar")
        } catch (e: SyncException) {
            assertEquals(SyncErrorKind.NOT_FOUND, e.kind)
        }
    }

    @Test
    fun `HTML do Google vira erro acionável`() {
        // HTML é retryable (M7): uma resposta por tentativa (3x com backoff).
        repeat(3) {
            server.enqueue(
                MockResponse().setResponseCode(200)
                    .setBody("<HTML><HEAD><TITLE>Erro</TITLE></HEAD></HTML>"),
            )
        }
        try {
            kotlinx.coroutines.runBlocking { client.ping(url, "segredo", "dev-1") }
            fail("deveria lançar")
        } catch (e: SyncException) {
            assertEquals(SyncErrorKind.HTML_ERROR, e.kind)
            assertEquals(3, server.requestCount)
        }
    }

    @Test
    fun `redirect 302 é seguido e o JSON do destino é lido`() {
        server.enqueue(
            MockResponse().setResponseCode(302)
                .addHeader("Location", server.url("/final").toString()),
        )
        server.enqueue(okJson("Destino final."))
        val res = kotlinx.coroutines.runBlocking { client.ping(url, "segredo", "dev-1") }
        assertEquals("Destino final.", res.message)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `reenvio repete o mesmo requestId e é idempotente`() {
        server.enqueue(MockResponse().setResponseCode(500).setBody("erro"))
        server.enqueue(okJson("Enviados: 1."))
        val res =
            kotlinx.coroutines.runBlocking {
                client.push(url, "segredo", "dev-1", SheetSchema.ACCOUNTS, emptyList(), false, requestId = "req-fixo")
            }
        assertEquals("Enviados: 1.", res.message)
        assertEquals(2, server.requestCount)
        val first = server.takeRequest(5, TimeUnit.SECONDS)!!.body.readUtf8()
        val second = server.takeRequest(5, TimeUnit.SECONDS)!!.body.readUtf8()
        assertTrue(first.contains("req-fixo"))
        assertTrue(second.contains("req-fixo"))
    }

    @Test
    fun `falha de rede inclui a causa entre colchetes`() {
        server.shutdown() // porta fechada → ConnectException
        try {
            kotlinx.coroutines.runBlocking { client.ping(url, "segredo", "dev-1") }
            fail("deveria lançar")
        } catch (e: SyncException) {
            assertEquals(SyncErrorKind.NETWORK, e.kind)
            assertTrue(e.message!!.contains("[ConnectException]"))
        }
    }

    @Test
    fun `timeout vira erro acionável`() {
        server.enqueue(
            MockResponse().setBodyDelay(3, TimeUnit.SECONDS)
                .setBody("""{"status":"ok","message":"lento"}"""),
        )
        val fastClient =
            SheetsClient(
                http =
                    OkHttpClient.Builder()
                        .callTimeout(1, TimeUnit.SECONDS)
                        .followRedirects(true)
                        .build(),
                maxRetries = 1,
            )
        try {
            kotlinx.coroutines.runBlocking { fastClient.ping(url, "segredo", "dev-1") }
            fail("deveria lançar")
        } catch (e: SyncException) {
            assertEquals(SyncErrorKind.TIMEOUT, e.kind)
        }
    }
}
