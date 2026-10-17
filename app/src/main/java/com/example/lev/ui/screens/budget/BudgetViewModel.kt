package com.example.lev.ui.screens.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lev.data.model.Budget
import com.example.lev.data.model.Category
import com.example.lev.data.repository.FinanceRepository
import com.example.lev.domain.BudgetEngine
import com.example.lev.domain.BudgetSnapshot
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.util.UUID

data class BudgetUiState(
    val isLoading: Boolean = true,
    val month: YearMonth = YearMonth.now(),
    val snapshots: List<BudgetSnapshot> = emptyList(),
    val expenseCategories: List<Category> = emptyList(),
    val hasAnyPlan: Boolean = false,
)

class BudgetViewModel(
    private val financeRepository: FinanceRepository,
) : ViewModel() {
    val uiState: StateFlow<BudgetUiState> =
        combine(
            financeRepository.getCategories(),
            financeRepository.getTransactions(),
            financeRepository.getBudgets(),
        ) { categories, transactions, budgets ->
            val now = YearMonth.now()
            val monthKey = now.toString()

            val plannedByCategory =
                budgets
                    .filter { it.month == monthKey }
                    .associate { it.categoryId to it.plannedCents }

            // Rollover: leftover from the previous month (planned + rollover - spent, min 0)
            val previousMonth = now.minusMonths(1)
            val previousKey = previousMonth.toString()
            val previousPlannedByCategory =
                budgets
                    .filter { it.month == previousKey }
                    .associate { it.categoryId to it.plannedCents }
            val previousSnapshots =
                BudgetEngine.calculateMonthlyBudget(
                    month = previousMonth,
                    categories = categories,
                    transactions = transactions,
                    plannedByCategory = previousPlannedByCategory,
                )
            val previousCarryoverByCategory =
                previousSnapshots
                    .filter { it.plannedCents > 0L }
                    .associate { it.categoryId to it.remainingCents.coerceAtLeast(0L) }

            val snapshots =
                BudgetEngine.calculateMonthlyBudget(
                    month = now,
                    categories = categories,
                    transactions = transactions,
                    plannedByCategory = plannedByCategory,
                    previousCarryoverByCategory = previousCarryoverByCategory,
                ).filter { it.plannedCents > 0L || it.rolloverCents > 0L || it.spentCents > 0L }

            BudgetUiState(
                isLoading = false,
                month = now,
                snapshots = snapshots,
                expenseCategories = categories.filter { it.kind == "EXPENSE" && it.parentId == null },
                hasAnyPlan = plannedByCategory.isNotEmpty(),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = BudgetUiState(),
        )

    fun savePlannedAmount(
        categoryId: String,
        plannedCents: Long,
    ) {
        viewModelScope.launch {
            val monthKey = uiState.value.month.toString()
            val now = System.currentTimeMillis()
            if (plannedCents <= 0L) {
                financeRepository.deleteBudget(monthKey, categoryId)
            } else {
                financeRepository.saveBudget(
                    Budget(
                        id = UUID.randomUUID().toString(),
                        month = monthKey,
                        categoryId = categoryId,
                        plannedCents = plannedCents,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            }
        }
    }
}
