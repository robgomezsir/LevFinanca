package com.example.lev.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class GoalProgress(
    val goalId: String,
    val targetCents: Long,
    val savedCents: Long,
    val remainingCents: Long,
    val percent: Int,
    val isAchieved: Boolean,
    val dailySuggestedCents: Long,
)

object GoalProgressCalculator {
    /**
     * Calculates progress for a goal from its target and the sum of contributions.
     * [dailySuggestedCents] is the amount needed per day to reach the target by [today]'s
     * reference (goal target date), or zero when no target date is set or the goal is achieved.
     */
    fun calculate(
        goalId: String,
        targetCents: Long,
        contributionsCents: Long,
        targetDate: LocalDate? = null,
        today: LocalDate = LocalDate.now(),
    ): GoalProgress {
        val safeTarget = targetCents.coerceAtLeast(0L)
        val saved = contributionsCents.coerceAtLeast(0L)
        val remaining = (safeTarget - saved).coerceAtLeast(0L)
        val percent =
            if (safeTarget == 0L) {
                if (saved > 0L) 100 else 0
            } else {
                ((saved.toDouble() / safeTarget.toDouble()) * 100).toInt().coerceAtMost(100)
            }
        val isAchieved = safeTarget > 0L && saved >= safeTarget

        val dailySuggested =
            if (isAchieved || targetDate == null || remaining == 0L) {
                0L
            } else {
                val daysLeft = ChronoUnit.DAYS.between(today, targetDate).coerceAtLeast(1L)
                ceilDiv(remaining, daysLeft)
            }

        return GoalProgress(
            goalId = goalId,
            targetCents = safeTarget,
            savedCents = saved,
            remainingCents = remaining,
            percent = percent,
            isAchieved = isAchieved,
            dailySuggestedCents = dailySuggested,
        )
    }

    private fun ceilDiv(
        value: Long,
        divisor: Long,
    ): Long = (value + divisor - 1) / divisor
}
