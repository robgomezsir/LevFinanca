package com.example.lev.domain

import com.example.lev.data.model.Transaction
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class BalanceCalculatorTest {
    private fun mockTx(
        type: String,
        amount: Long,
        status: String,
        account: String,
        destAccount: String? = null,
        date: LocalDate = LocalDate.now(),
    ): Transaction {
        return Transaction(
            id = "1", type = type, description = "Test", amountCents = amount,
            date = date, status = status, accountId = account,
            destinationAccountId = destAccount, createdAt = 0L, updatedAt = 0L,
        )
    }

    @Test
    fun `calculate current balance with income and expense`() {
        val txs =
            listOf(
                mockTx("INCOME", 1000L, "PAID", "A1"),
                mockTx("EXPENSE", 300L, "PAID", "A1"),
                // Should ignore pending expenses.
                mockTx("EXPENSE", 200L, "PENDING", "A1"),
            )
        val bal = BalanceCalculator.calculateCurrentBalance(500L, txs, "A1")
        // 500 + 1000 - 300
        assertEquals(1200L, bal)
    }

    @Test
    fun `calculate current balance with transfer`() {
        val txs =
            listOf(
                mockTx("TRANSFER", 200L, "PAID", "A1", destAccount = "A2"),
            )
        val balA1 = BalanceCalculator.calculateCurrentBalance(500L, txs, "A1")
        val balA2 = BalanceCalculator.calculateCurrentBalance(100L, txs, "A2")

        assertEquals(300L, balA1)
        assertEquals(300L, balA2)
    }

    @Test
    fun `credit card account should not count as available cash`() {
        val txs =
            listOf(
                mockTx("EXPENSE", 200L, "PAID", "C1"),
            )
        val bal = BalanceCalculator.calculateCurrentBalance(500L, txs, "C1", accountType = "CREDIT_CARD")
        assertEquals(500L, bal)
    }
}
