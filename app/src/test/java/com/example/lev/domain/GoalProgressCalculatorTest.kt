package com.example.lev.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GoalProgressCalculatorTest {
    private val today = LocalDate.of(2026, 10, 10)

    @Test
    fun `empty goal has zero progress`() {
        val progress =
            GoalProgressCalculator.calculate(
                goalId = "g1",
                targetCents = 100_000,
                contributionsCents = 0,
                today = today,
            )

        assertEquals(0, progress.percent)
        assertEquals(100_000, progress.remainingCents)
        assertFalse(progress.isAchieved)
    }

    @Test
    fun `partial progress is half when half saved`() {
        val progress =
            GoalProgressCalculator.calculate(
                goalId = "g1",
                targetCents = 100_000,
                contributionsCents = 50_000,
                today = today,
            )

        assertEquals(50, progress.percent)
        assertEquals(50_000, progress.remainingCents)
        assertFalse(progress.isAchieved)
    }

    @Test
    fun `achieved goal caps percent at 100`() {
        val progress =
            GoalProgressCalculator.calculate(
                goalId = "g1",
                targetCents = 100_000,
                contributionsCents = 120_000,
                today = today,
            )

        assertEquals(100, progress.percent)
        assertEquals(0, progress.remainingCents)
        assertTrue(progress.isAchieved)
        assertEquals(0L, progress.dailySuggestedCents)
    }

    @Test
    fun `daily suggestion splits remaining over days left rounding up`() {
        // 10 days to reach R$ 100,00 → R$ 10,00/day
        val progress =
            GoalProgressCalculator.calculate(
                goalId = "g1",
                targetCents = 10_000,
                contributionsCents = 0,
                targetDate = today.plusDays(10),
                today = today,
            )

        assertEquals(1_000L, progress.dailySuggestedCents)
    }

    @Test
    fun `daily suggestion rounds up when not divisible`() {
        // R$ 10,00 over 3 days → 3,34/day (ceil)
        val progress =
            GoalProgressCalculator.calculate(
                goalId = "g1",
                targetCents = 10_000,
                contributionsCents = 0,
                targetDate = today.plusDays(3),
                today = today,
            )

        assertEquals(3_334L, progress.dailySuggestedCents)
    }

    @Test
    fun `past due date uses minimum one day`() {
        val progress =
            GoalProgressCalculator.calculate(
                goalId = "g1",
                targetCents = 5_000,
                contributionsCents = 1_000,
                targetDate = today.minusDays(5),
                today = today,
            )

        assertEquals(4_000L, progress.dailySuggestedCents)
    }

    @Test
    fun `no target date means no daily suggestion`() {
        val progress =
            GoalProgressCalculator.calculate(
                goalId = "g1",
                targetCents = 5_000,
                contributionsCents = 0,
                targetDate = null,
                today = today,
            )

        assertEquals(0L, progress.dailySuggestedCents)
    }

    @Test
    fun `negative contributions are ignored`() {
        val progress =
            GoalProgressCalculator.calculate(
                goalId = "g1",
                targetCents = 100_000,
                contributionsCents = -5_000,
                today = today,
            )

        assertEquals(0, progress.savedCents)
        assertEquals(100_000, progress.remainingCents)
    }
}
