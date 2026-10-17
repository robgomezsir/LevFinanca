package com.example.lev.domain

import com.example.lev.data.model.Transaction
import java.time.LocalDate
import java.time.YearMonth

data class ForecastOutcome(
    val currentBalanceCents: Long,
    val projectedEndOfMonthCents: Long,
    val averageDailyVariableExpenseCents: Long,
    val dailySafeSpendCents: Long,
    val daysRemainingInMonth: Int,
)

data class NetWorthSnapshot(
    val month: YearMonth,
    val assetsCents: Long,
    val liabilitiesCents: Long,
    val netWorthCents: Long,
)

data class Insight(
    val title: String,
    val description: String,
    val sourceScreen: String,
)

object ForecastEngine {
    fun calculate(
        currentBalance: Long,
        pendingTransactions: List<Transaction>,
        historicalVariableExpenses: List<Long>,
        month: YearMonth,
        referenceDate: LocalDate,
    ): ForecastOutcome {
        val daysRemaining = (month.lengthOfMonth() - referenceDate.dayOfMonth + 1).coerceAtLeast(1)
        val pendingIncome =
            pendingTransactions.filter {
                it.status == "PENDING" && it.type == "INCOME" &&
                    it.date.year == month.year && it.date.monthValue == month.monthValue
            }.sumOf { it.amountCents }

        val pendingExpenses =
            pendingTransactions.filter {
                it.status == "PENDING" && it.type == "EXPENSE" &&
                    it.date.year == month.year && it.date.monthValue == month.monthValue
            }.sumOf { it.amountCents }

        val averageDailyVariableExpense =
            if (historicalVariableExpenses.isEmpty()) {
                0L
            } else {
                historicalVariableExpenses.average().toLong()
            }

        val projectedEndOfMonth =
            currentBalance + pendingIncome - pendingExpenses -
                (averageDailyVariableExpense * daysRemaining)
        val dailySafeSpend =
            if (historicalVariableExpenses.isEmpty()) {
                0L
            } else {
                val leftoverForDay = (currentBalance + pendingIncome - pendingExpenses) / daysRemaining.toLong()
                (leftoverForDay - averageDailyVariableExpense).coerceAtLeast(0L)
            }

        return ForecastOutcome(
            currentBalanceCents = currentBalance,
            projectedEndOfMonthCents = projectedEndOfMonth,
            averageDailyVariableExpenseCents = averageDailyVariableExpense,
            dailySafeSpendCents = dailySafeSpend,
            daysRemainingInMonth = daysRemaining,
        )
    }
}

object NetWorthEngine {
    fun generate(
        month: YearMonth,
        assetValues: Map<String, Long>,
        liabilityValues: Map<String, Long>,
    ): NetWorthSnapshot {
        val assets = assetValues.values.sum()
        val liabilities = liabilityValues.values.sum()
        return NetWorthSnapshot(
            month = month,
            assetsCents = assets,
            liabilitiesCents = liabilities,
            netWorthCents = assets - liabilities,
        )
    }
}

object InsightEngine {
    fun generate(
        month: YearMonth,
        totalExpenses: Long,
        previousMonthExpenses: Long,
        budgetUsed: Int,
        largestExpense: Long,
        topCategory: String,
    ): List<Insight> {
        val insights = mutableListOf<Insight>()

        if (previousMonthExpenses > 0L) {
            val delta = ((totalExpenses.toDouble() / previousMonthExpenses.toDouble()) - 1.0) * 100.0
            if (delta <= -10.0) {
                val percentChange = kotlin.math.abs(delta).toInt()
                insights +=
                    Insight(
                        title = "economia de $percentChange% em relação ao mês anterior",
                        description = "Seus gastos de $month ficaram $percentChange% abaixo do mês anterior.",
                        sourceScreen = "dashboard",
                    )
            }
            if (delta >= 10.0) {
                insights +=
                    Insight(
                        title = "gastos acima da média histórica em ${delta.toInt()}%",
                        description = "O mês de $month está acima do mês anterior em ${delta.toInt()}%.",
                        sourceScreen = "reports",
                    )
            }
        }

        if (budgetUsed >= 100) {
            insights +=
                Insight(
                    title = "orçamento já ultrapassou 100%",
                    description = "Uma ou mais categorias já passaram do limite planejado neste mês.",
                    sourceScreen = "budget",
                )
        }

        if (budgetUsed in 80..99) {
            insights +=
                Insight(
                    title = "orçamento em alerta: $budgetUsed% usado",
                    description = "O orçamento já está em nível de atenção e exige revisão.",
                    sourceScreen = "budget",
                )
        }

        if (largestExpense > 0L) {
            insights +=
                Insight(
                    title = "maior gasto do mês em $topCategory",
                    description =
                        "A maior despesa do período foi de " +
                            "${MoneyFormatter.format(largestExpense)} em $topCategory.",
                    sourceScreen = "category-detail",
                )
        }

        return insights.take(2)
    }
}
