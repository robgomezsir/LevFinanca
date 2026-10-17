package com.example.lev.domain

import com.example.lev.data.model.Account
import com.example.lev.data.model.Invoice
import com.example.lev.data.model.Transaction
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class AccountBalancesTest {
    private fun account(
        id: String,
        type: String,
        initial: Long = 0L,
        include: Boolean = true,
        limit: Long? = null,
    ): Account =
        Account(
            id = id,
            name = id,
            type = type,
            initialBalanceCents = initial,
            currency = "BRL",
            color = "#000",
            icon = "i",
            includeInTotals = include,
            creditLimitCents = limit,
            closingDay = 28,
            dueDay = 10,
            createdAt = 0L,
            updatedAt = 0L,
        )

    private fun tx(
        id: String,
        type: String,
        amount: Long,
        accountId: String,
        status: String = "PAID",
        dest: String? = null,
        date: LocalDate = LocalDate.of(2026, 5, 10),
    ): Transaction =
        Transaction(
            id = id,
            type = type,
            description = id,
            amountCents = amount,
            date = date,
            status = status,
            accountId = accountId,
            destinationAccountId = dest,
            createdAt = 0L,
            updatedAt = 0L,
        )

    @Test
    fun `saldo soma pagos e ignora pendentes`() {
        val acc = account("c1", "CHECKING", initial = 10000L)
        val txs =
            listOf(
                tx("t1", "INCOME", 5000L, "c1"),
                tx("t2", "EXPENSE", 2000L, "c1"),
                tx("t3", "EXPENSE", 9000L, "c1", status = "PENDING"),
                tx("t4", "ADJUSTMENT", 500L, "c1"),
            )
        assertEquals(13500L, AccountBalances.currentBalance(acc, txs))
    }

    @Test
    fun `transferência soma em uma conta e subtrai na outra`() {
        val a = account("a", "CHECKING")
        val b = account("b", "SAVINGS")
        val txs = listOf(tx("t1", "TRANSFER", 3000L, "a", dest = "b"))
        assertEquals(-3000L, AccountBalances.currentBalance(a, txs))
        assertEquals(3000L, AccountBalances.currentBalance(b, txs))
    }

    @Test
    fun `cartão em aberto exclui meses pagos`() {
        val today = LocalDate.now()
        val card = account("card", "CREDIT_CARD", limit = 100000L)
        val txs = listOf(tx("t1", "EXPENSE", 15000L, "card", date = today))
        assertEquals(15000L, AccountBalances.cardOpenTotal(card, txs, emptyList()))

        val ref =
            InvoiceCycle.calculateInvoiceForPurchase(
                purchaseDate = today,
                closingDay = 28,
                dueDay = 10,
                closingInclusive = true,
            ).referenceMonth
        val paid =
            Invoice(
                id = "inv",
                accountId = "card",
                referenceMonth = ref,
                closingDate = ref,
                dueDate = ref,
                status = "PAID",
                totalCents = 15000L,
                createdAt = 0L,
                updatedAt = 0L,
            )
        assertEquals(0L, AccountBalances.cardOpenTotal(card, txs, listOf(paid)))
    }

    @Test
    fun `patrimônio cruza saldos reais com dívidas em aberto`() {
        val checking = account("c1", "CHECKING", initial = 10000L)
        val hidden = account("c2", "CHECKING", initial = 50000L, include = false)
        val card = account("card", "CREDIT_CARD", limit = 100000L)
        val today = LocalDate.now()
        val txs =
            listOf(
                tx("t1", "EXPENSE", 2000L, "c1"),
                tx("t2", "EXPENSE", 7000L, "card", date = today),
            )
        val worth = AccountBalances.netWorth(listOf(checking, hidden, card), txs, emptyList())
        assertEquals(8000L, worth.assetsCents)
        assertEquals(7000L, worth.liabilitiesCents)
        assertEquals(1000L, worth.netCents)
    }
}
