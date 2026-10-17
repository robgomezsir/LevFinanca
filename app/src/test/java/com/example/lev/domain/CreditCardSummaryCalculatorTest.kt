package com.example.lev.domain

import com.example.lev.data.model.Transaction
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CreditCardSummaryCalculatorTest {
    private fun mockTx(
        id: String,
        amount: Long,
        account: String,
        installmentNumber: Int? = null,
        installmentTotal: Int? = null,
        status: String = "PAID",
    ): Transaction =
        Transaction(
            id = id,
            type = "EXPENSE",
            description = "Compra",
            amountCents = amount,
            date = LocalDate.of(2026, 10, 5),
            status = status,
            accountId = account,
            installmentNumber = installmentNumber,
            installmentTotal = installmentTotal,
            createdAt = 0L,
            updatedAt = 0L,
        )

    @Test
    fun `calculate available credit for card account`() {
        val txs =
            listOf(
                mockTx("1", 1200L, "CARD"),
                mockTx("2", 3000L, "CARD"),
            )

        val summary =
            CreditCardSummaryCalculator.summarize(
                creditLimitCents = 10000L,
                transactions = txs,
                accountId = "CARD",
            )

        assertEquals(10000L, summary.creditLimitCents)
        assertEquals(4200L, summary.openInvoicesTotalCents)
        assertEquals(5800L, summary.availableCreditCents)
    }

    @Test
    fun `sum future installment commitment`() {
        val txs =
            listOf(
                mockTx("1", 3000L, "CARD", installmentNumber = 1, installmentTotal = 3, status = "PENDING"),
                mockTx("2", 3000L, "CARD", installmentNumber = 2, installmentTotal = 3, status = "PENDING"),
                mockTx("3", 3000L, "CARD", installmentNumber = 3, installmentTotal = 3, status = "PENDING"),
            )

        val commitment = CreditCardSummaryCalculator.futureInstallmentsCommitment(txs, "CARD")
        assertEquals(6000L, commitment)
    }
}
