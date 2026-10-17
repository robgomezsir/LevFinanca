package com.example.lev.domain

import com.example.lev.data.model.Account
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SheetMappersTest {
    @Test
    fun `dinheiro em todos os formatos BR`() {
        assertEquals(123456L, parseMoneyStrict("R$ 1.234,56"))
        assertEquals(123456L, parseMoneyStrict("1234,56"))
        assertEquals(123456L, parseMoneyStrict("1234.56"))
        assertEquals(123400L, parseMoneyStrict("1.234"))
        assertEquals(0L, parseMoneyStrict("0"))
        assertNull(parseMoneyStrict(""))
        assertNull(parseMoneyStrict("   "))
        assertNull(parseMoneyStrict("abc"))
        assertNull(parseMoneyStrict("sem valor"))
    }

    @Test
    fun `centavos viram decimal com ponto`() {
        assertEquals("1500.50", 150050L.toSheetMoney())
        assertEquals("-5.00", (-500L).toSheetMoney())
        assertEquals("0.00", 0L.toSheetMoney())
    }

    @Test
    fun `datas em ddMMyyyy ISO e curta`() {
        assertEquals(LocalDate.of(2026, 3, 15), parseSheetDate("15/03/2026"))
        assertEquals(LocalDate.of(2026, 3, 15), parseSheetDate("2026-03-15"))
        assertEquals(LocalDate.of(LocalDate.now().year, 3, 15), parseSheetDate("15/03"))
        assertNull(parseSheetDate("32/13/2026"))
        assertNull(parseSheetDate("ontem"))
        assertNull(parseSheetDate(""))
        assertEquals("05/01/2026", LocalDate.of(2026, 1, 5).toSheetDate())
    }

    @Test
    fun `conta faz round-trip pela linha`() {
        val now = 1700000000000L
        val account =
            Account(
                id = "acc-1",
                name = "Nubank",
                type = "CHECKING",
                initialBalanceCents = 150050L,
                currency = "BRL",
                color = "#00658F",
                icon = "wallet",
                includeInTotals = true,
                archived = false,
                sortOrder = 2,
                createdAt = now - 1000,
                updatedAt = now,
            )
        val row = account.toSheetRow()
        assertEquals("Conta corrente", row["Tipo"])
        assertEquals("1500.50", row["Saldo Inicial (R$)"])

        val parsed = rowToAccount(row, 2)
        assertNull(parsed.error)
        val back = parsed.value!!
        assertEquals(account.copy(createdAt = now), back)
    }

    @Test
    fun `rótulos toleram caixa e código`() {
        val base =
            Account(
                id = "a", name = "C", type = "CHECKING", initialBalanceCents = 100L,
                currency = "BRL", color = "#000", icon = "i",
                createdAt = 1L, updatedAt = 1L,
            ).toSheetRow()
        val tx =
            mapOf(
                "ID" to "t1",
                "Tipo" to "TRANSFERÊNCIA",
                "Descrição" to "x",
                "Valor (R$)" to "10,00",
                "Data" to "01/02/2026",
                "Status" to "pago",
                "Conta ID" to "a",
                "Atualizado Em" to "1700000000000",
            )
        val parsed = rowToTransaction(tx, 3)
        assertNull(parsed.error)
        assertEquals("TRANSFER", parsed.value!!.type)
        assertEquals("PAID", parsed.value!!.status)
        assertEquals(1000L, parsed.value!!.amountCents)
        assertEquals(base["ID"], "a")
    }

    @Test
    fun `linhas inválidas voltam com número da linha`() {
        val badMoney =
            mapOf(
                "ID" to "t1",
                "Tipo" to "Despesa",
                "Descrição" to "x",
                "Valor (R$)" to "invalido",
                "Data" to "01/02/2026",
                "Status" to "Pago",
                "Conta ID" to "a",
                "Atualizado Em" to "1",
            )
        val r1 = rowToTransaction(badMoney, 7)
        assertNull(r1.value)
        assertNotNull(r1.error)
        assertTrue(r1.error!!.contains("linha 7"))

        val badType = badMoney + mapOf("Valor (R$)" to "10,00", "Tipo" to "Doação")
        val r2 = rowToTransaction(badType, 8)
        assertNull(r2.value)
        assertTrue(r2.error!!.contains("linha 8"))

        val noId = badMoney + mapOf("ID" to "", "Valor (R$)" to "10,00")
        val r3 = rowToTransaction(noId, 9)
        assertNull(r3.value)
    }

    @Test
    fun `divisão tem id estável sem coluna ID`() {
        val a = splitIdOf("tx", "cat", 1000L, "")
        val b = splitIdOf("tx", "cat", 1000L, "")
        val c = splitIdOf("tx", "cat", 2000L, "")
        assertEquals(a, b)
        assertTrue(a != c)
    }
}
