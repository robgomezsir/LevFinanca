package com.example.lev.domain

import com.example.lev.data.model.Transaction
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CardAccountSummaryCalculatorTest {
    private fun tx(
        id: String,
        amount: Long,
        accountId: String,
        installmentNumber: Int? = null,
        installmentTotal: Int? = null,
        status: String = "PAID",
    ) = Transaction(
        id = id,
        type = "EXPENSE",
        description = "Compra",
        amountCents = amount,
        date = LocalDate.of(2026, 10, 5),
        status = status,
        accountId = accountId,
        installmentNumber = installmentNumber,
        installmentTotal = installmentTotal,
        createdAt = 0L,
        updatedAt = 0L,
    )

    @Test
    fun `summarize card status with limit and future installments`() {
        val txs =
            listOf(
                tx("1", 1200L, "CARD"),
                tx("2", 1800L, "CARD"),
                tx("3", 3000L, "CARD", installmentNumber = 1, installmentTotal = 3, status = "PENDING"),
                tx("4", 3000L, "CARD", installmentNumber = 2, installmentTotal = 3, status = "PENDING"),
            )

        val summary =
            CardAccountSummaryCalculator.summarize(
                cardLimitCents = 10000L,
                transactions = txs,
                accountId = "CARD",
                currentInvoiceTotalCents = 3000L,
                nextDueDate = LocalDate.of(2026, 11, 5),
            )

        assertEquals(10000L, summary.limitCents)
        assertEquals(3000L, summary.usedCents)
        assertEquals(7000L, summary.availableCents)
        assertEquals(6000L, summary.futureInstallmentsCents)
        assertEquals(3000L, summary.currentInvoiceTotalCents)
        assertEquals(LocalDate.of(2026, 11, 5), summary.nextDueDate)
    }
}
