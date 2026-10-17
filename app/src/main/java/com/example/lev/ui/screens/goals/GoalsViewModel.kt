package com.example.lev.ui.screens.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lev.data.model.Goal
import com.example.lev.data.model.GoalContribution
import com.example.lev.data.repository.FinanceRepository
import com.example.lev.domain.GoalProgress
import com.example.lev.domain.GoalProgressCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

data class GoalListItem(
    val goal: Goal,
    val progress: GoalProgress,
    val lastContributionDate: LocalDate?,
)

data class GoalsUiState(
    val isLoading: Boolean = true,
    val goals: List<GoalListItem> = emptyList(),
)

class GoalsViewModel(
    private val financeRepository: FinanceRepository,
) : ViewModel() {
    val uiState: StateFlow<GoalsUiState> =
        combine(
            financeRepository.getGoals(),
            financeRepository.getGoalContributions(),
        ) { goals, contributions ->
            val items =
                goals
                    .filter { it.status != "ARCHIVED" }
                    .map { goal ->
                        val goalContributions = contributions.filter { it.goalId == goal.id }
                        val savedCents = goalContributions.sumOf { it.amountCents }
                        val progress =
                            GoalProgressCalculator.calculate(
                                goalId = goal.id,
                                targetCents = goal.targetCents,
                                contributionsCents = savedCents,
                                targetDate = goal.targetDate,
                            )
                        GoalListItem(
                            goal = goal,
                            progress = progress,
                            lastContributionDate = goalContributions.maxOfOrNull { it.date },
                        )
                    }
            GoalsUiState(isLoading = false, goals = items)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = GoalsUiState(),
        )

    fun createGoal(
        name: String,
        targetCents: Long,
        targetDate: LocalDate?,
    ) {
        viewModelScope.launch {
            if (targetCents <= 0L) return@launch
            val now = System.currentTimeMillis()
            financeRepository.saveGoal(
                Goal(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    targetCents = targetCents,
                    targetDate = targetDate,
                    color = "#2196F3",
                    icon = "savings",
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }
    }

    fun addContribution(
        goalId: String,
        amountCents: Long,
    ) {
        viewModelScope.launch {
            if (amountCents <= 0L) return@launch
            val now = System.currentTimeMillis()
            financeRepository.saveGoalContribution(
                GoalContribution(
                    id = UUID.randomUUID().toString(),
                    goalId = goalId,
                    amountCents = amountCents,
                    date = LocalDate.now(),
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }
    }

    fun deleteGoal(id: String) {
        viewModelScope.launch {
            financeRepository.deleteGoal(id)
        }
    }
}
