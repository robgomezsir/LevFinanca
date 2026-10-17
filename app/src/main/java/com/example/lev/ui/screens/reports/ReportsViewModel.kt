package com.example.lev.ui.screens.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lev.data.repository.FinanceRepository
import com.example.lev.domain.AccountBalances
import com.example.lev.domain.BudgetEngine
import com.example.lev.domain.BudgetSnapshot
import com.example.lev.domain.Insight
import com.example.lev.domain.InsightEngine
import com.example.lev.domain.NetWorthEngine
import com.example.lev.domain.NetWorthSnapshot
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth

data class ReportsUiState(
    val netWorth: NetWorthSnapshot? = null,
    val insights: List<Insight> = emptyList(),
    val budgetSnapshots: List<BudgetSnapshot> = emptyList(),
    val isLoading: Boolean = true,
)

class ReportsViewModel(
    private val financeRepository: FinanceRepository,
) : ViewModel() {
    val uiState: StateFlow<ReportsUiState> =
        combine(
            financeRepository.getAccounts(),
            financeRepository.getTransactions(),
            financeRepository.getCategories(),
            financeRepository.getAllInvoices(),
        ) { accounts, transactions, categories, invoices ->
            val now = YearMonth.now()
            val currentMonthTransactions =
                transactions.filter {
                    it.date.year == now.year && it.date.monthValue == now.monthValue
                }

            val totalExpenses =
                currentMonthTransactions
                    .filter { it.status == "PAID" && it.type == "EXPENSE" }
                    .sumOf { it.amountCents }

            val previousMonth = now.minusMonths(1)
            val previousMonthExpenses =
                transactions
                    .filter {
                        it.status == "PAID" &&
                            it.type == "EXPENSE" &&
                            it.date.year == previousMonth.year &&
                            it.date.monthValue == previousMonth.monthValue
                    }
                    .sumOf { it.amountCents }

            val largestExpense =
                currentMonthTransactions
                    .filter { it.status == "PAID" && it.type == "EXPENSE" }
                    .maxOfOrNull { it.amountCents }
                    ?: 0L

            val categoryBreakdown =
                currentMonthTransactions
                    .filter { it.type == "EXPENSE" && it.status == "PAID" }
                    .groupBy { it.categoryId ?: "unknown" }
                    .mapValues { it.value.sumOf { tx -> tx.amountCents } }

            val topCategory =
                categoryBreakdown.maxByOrNull { it.value }?.key?.let { categoryId ->
                    categories.firstOrNull { it.id == categoryId }?.name ?: "Outras"
                } ?: "N/A"

            val budgetSnapshots =
                BudgetEngine.calculateMonthlyBudget(
                    month = now,
                    categories = categories,
                    transactions = transactions,
                )

            val budgetUsed =
                if (budgetSnapshots.isNotEmpty()) {
                    val totalPlanned = budgetSnapshots.sumOf { it.plannedCents + it.rolloverCents }
                    if (totalPlanned > 0) {
                        (budgetSnapshots.sumOf { it.spentCents } * 100 / totalPlanned).toInt()
                    } else {
                        0
                    }
                } else {
                    0
                }

            val insights =
                InsightEngine.generate(
                    month = now,
                    totalExpenses = totalExpenses,
                    previousMonthExpenses = previousMonthExpenses,
                    budgetUsed = budgetUsed,
                    largestExpense = largestExpense,
                    topCategory = topCategory,
                )

            val assetAccounts =
                accounts.filter { it.type in listOf("CHECKING", "SAVINGS", "CASH", "INVESTMENT") }
            val liabilityAccounts = accounts.filter { it.type == "LOAN" || it.type == "CREDIT_CARD" }
            val assetValues = assetAccounts.associate { it.id to it.initialBalanceCents }
            val liabilityValues =
                liabilityAccounts.associate { it.id to (it.creditLimitCents ?: 0L) }

            // Patrimônio com os mesmos saldos reais do Painel/Contas (Base §5.3/§5.5).
            val worth = AccountBalances.netWorth(accounts, transactions, invoices)
            val netWorth =
                NetWorthEngine.generate(
                    month = now,
                    assetValues = mapOf("total" to worth.assetsCents),
                    liabilityValues = mapOf("total" to worth.liabilitiesCents),
                )

            ReportsUiState(
                netWorth = netWorth,
                insights = insights,
                budgetSnapshots = budgetSnapshots,
                isLoading = false,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ReportsUiState(),
        )
}
