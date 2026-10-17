package com.example.lev.domain

import com.example.lev.data.model.Transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class M6ReportEngineTest {
    private fun tx(
        id: String,
        amount: Long,
        categoryId: String = "food",
        date: LocalDate,
        type: String = "EXPENSE",
        status: String = "PAID",
    ) = Transaction(
        id = id,
        type = type,
        description = "tx-$id",
        amountCents = amount,
        date = date,
        status = status,
        accountId = "cash",
        categoryId = categoryId,
        createdAt = 0L,
        updatedAt = 0L,
    )

    @Test
    fun `forecast engine projects end of month using pending flows and last 90 day average`() {
        val transactions =
            listOf(
                tx("p1", 200000L, date = LocalDate.of(2026, 8, 10), type = "INCOME", status = "PENDING"),
                tx("e1", 150000L, date = LocalDate.of(2026, 8, 15), status = "PENDING"),
                tx("e2", 30000L, date = LocalDate.of(2026, 8, 20), status = "PAID"),
            )
        val historical =
            listOf(
                10000L,
                20000L,
                15000L,
                20000L,
                9000L,
            )

        val forecast =
            ForecastEngine.calculate(
                currentBalance = 500000L,
                pendingTransactions = transactions,
                historicalVariableExpenses = historical,
                month = YearMonth.of(2026, 8),
                referenceDate = LocalDate.of(2026, 8, 14),
            )

        assertEquals(283600L, forecast.projectedEndOfMonthCents)
        assertEquals(14800L, forecast.averageDailyVariableExpenseCents)
        assertEquals(15755L, forecast.dailySafeSpendCents)
    }

    @Test
    fun `forecast engine handles months with no history`() {
        val forecast =
            ForecastEngine.calculate(
                currentBalance = 1000000L,
                pendingTransactions = emptyList(),
                historicalVariableExpenses = emptyList(),
                month = YearMonth.of(2026, 9),
                referenceDate = LocalDate.of(2026, 9, 1),
            )

        assertEquals(1000000L, forecast.projectedEndOfMonthCents)
        assertEquals(0L, forecast.averageDailyVariableExpenseCents)
        assertEquals(0L, forecast.dailySafeSpendCents)
    }

    @Test
    fun `net worth snapshot is idempotent and aggregates assets and liabilities`() {
        val first =
            NetWorthEngine.generate(
                month = YearMonth.of(2026, 8),
                assetValues = mapOf("cash" to 200000L, "investments" to 500000L),
                liabilityValues = mapOf("card" to 150000L, "loan" to 300000L),
            )

        val second =
            NetWorthEngine.generate(
                month = YearMonth.of(2026, 8),
                assetValues = mapOf("cash" to 200000L, "investments" to 500000L),
                liabilityValues = mapOf("card" to 150000L, "loan" to 300000L),
            )

        assertEquals(700000L, first.assetsCents)
        assertEquals(450000L, first.liabilitiesCents)
        assertEquals(250000L, first.netWorthCents)
        assertEquals(first, second)
    }

    @Test
    fun `insight engine emits positive and negative signals deterministically`() {
        val positive =
            InsightEngine.generate(
                month = YearMonth.of(2026, 8),
                totalExpenses = 1500000L,
                previousMonthExpenses = 2000000L,
                budgetUsed = 82,
                largestExpense = 500000L,
                topCategory = "food",
            )

        assertTrue(
            positive.any {
                it.title.contains("economia") || it.title.contains("baixo")
            },
        )

        val negative =
            InsightEngine.generate(
                month = YearMonth.of(2026, 8),
                totalExpenses = 2500000L,
                previousMonthExpenses = 1500000L,
                budgetUsed = 110,
                largestExpense = 900000L,
                topCategory = "travel",
            )

        assertTrue(
            negative.any {
                it.title.contains("acima") || it.title.contains("orçamento")
            },
        )
    }
}
