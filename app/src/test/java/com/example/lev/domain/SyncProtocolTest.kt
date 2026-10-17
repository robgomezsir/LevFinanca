package com.example.lev.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class SyncProtocolTest {
    @Test
    fun `sig é HMAC-SHA256 de ts action requestId`() {
        val secret = "segredo-teste"
        val sig = SyncProtocol.sign(secret, 1700000000000L, "ping", "req-1")

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val expected =
            mac.doFinal("1700000000000.ping.req-1".toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }

        assertEquals(expected, sig)
    }

    @Test
    fun `segredo errado gera sig diferente`() {
        val a = SyncProtocol.sign("certo", 1L, "ping", "r")
        val b = SyncProtocol.sign("errado", 1L, "ping", "r")
        assertFalse(a == b)
    }

    @Test
    fun `envelope tem todos os campos comuns e sig consistente`() {
        val env = SyncProtocol.buildEnvelope("sync_push", "s", "dev-1")
        assertEquals("sync_push", env["action"])
        assertEquals("dev-1", env["deviceId"])
        assertEquals(SyncProtocol.PROTOCOL_VERSION, env["schemaVersion"])
        val recomputed =
            SyncProtocol.sign("s", env["ts"] as Long, "sync_push", env["requestId"] as String)
        assertEquals(recomputed, env["sig"])
    }

    @Test
    fun `janela de ts aceita agora e rejeita expirado`() {
        val now = System.currentTimeMillis()
        assertTrue(SyncProtocol.isTimestampFresh(now, now))
        assertFalse(SyncProtocol.isTimestampFresh(now - 10 * 60 * 1000L, now))
        assertFalse(SyncProtocol.isTimestampFresh(now + 10 * 60 * 1000L, now))
    }

    @Test
    fun `parseResponse ok lê linhas e paginação`() {
        val json =
            """{"status":"ok","message":"Lidos: 2.","counts":{"read":2},"serverTime":1,
               |"scriptVersion":"1.0.0","rows":[{"ID":"a"},{"ID":"b"}],
               |"hasMore":true,"nextOffset":2,"total":3}
            """.trimMargin()
        val res = SyncProtocol.parseResponse(json)
        assertEquals(2, res.rows.size)
        assertEquals("a", res.rows[0]["ID"])
        assertTrue(res.hasMore)
        assertEquals(2, res.nextOffset)
        assertEquals(2L, res.counts["read"])
    }

    @Test
    fun `parseResponse erro mapeia código do servidor`() {
        val json = """{"status":"error","code":"INVALID_SECRET","message":"Assinatura inválida."}"""
        try {
            SyncProtocol.parseResponse(json)
            fail("deveria lançar")
        } catch (e: SyncException) {
            assertEquals(SyncErrorKind.UNAUTHORIZED, e.kind)
        }
    }

    @Test
    fun `parseResponse com script antigo falha acionável`() {
        val json = """{"status":"ok","message":"Pong.","scriptVersion":"0.5"}"""
        try {
            SyncProtocol.parseResponse(json)
            fail("deveria lançar")
        } catch (e: SyncException) {
            assertEquals(SyncErrorKind.OLD_SCRIPT, e.kind)
            assertTrue(e.message!!.contains("Nova versão"))
        }
    }

    @Test
    fun `classifyHttpError cobre 401 404 5xx e HTML`() {
        assertEquals(SyncErrorKind.UNAUTHORIZED, SyncProtocol.classifyHttpError(403, ""))
        assertEquals(SyncErrorKind.NOT_FOUND, SyncProtocol.classifyHttpError(404, ""))
        assertEquals(SyncErrorKind.SERVER, SyncProtocol.classifyHttpError(500, ""))
        assertEquals(SyncErrorKind.HTML_ERROR, SyncProtocol.classifyHttpError(200, "<HTML>erro</HTML>"))
        assertEquals(SyncErrorKind.NETWORK, SyncProtocol.classifyHttpError(null, null))
    }

    @Test
    fun `mensagens acionáveis citam o caminho de correção`() {
        assertTrue(SyncErrorKind.UNAUTHORIZED.actionableMessage().contains("Qualquer pessoa"))
        assertTrue(SyncErrorKind.NOT_FOUND.actionableMessage().contains("/exec"))
        assertTrue(SyncErrorKind.OLD_SCRIPT.actionableMessage().contains("Nova versão"))
    }
}
