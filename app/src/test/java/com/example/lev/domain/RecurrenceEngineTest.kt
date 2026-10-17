package com.example.lev.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RecurrenceEngineTest {
    @Test
    fun `monthly recurrence generates the next three months`() {
        val occurrences =
            RecurrenceEngine.generate(
                recurrenceId = "rent-1",
                startDate = LocalDate.of(2026, 1, 10),
                frequency = RecurrenceFrequency.MONTHLY,
                interval = 1,
                horizonMonths = 3,
                amountCents = 250000L,
            )

        assertEquals(4, occurrences.size)
        assertEquals(
            listOf(
                LocalDate.of(2026, 1, 10),
                LocalDate.of(2026, 2, 10),
                LocalDate.of(2026, 3, 10),
                LocalDate.of(2026, 4, 10),
            ),
            occurrences.map { it.date },
        )
        assertEquals(listOf(250000L, 250000L, 250000L, 250000L), occurrences.map { it.amountCents })
    }

    @Test
    fun `generate is idempotent for the same recurrence id and date`() {
        val first =
            RecurrenceEngine.generate(
                recurrenceId = "rent-1",
                startDate = LocalDate.of(2026, 1, 31),
                frequency = RecurrenceFrequency.MONTHLY,
                interval = 1,
                horizonMonths = 2,
                amountCents = 200000L,
            )

        val second =
            RecurrenceEngine.generate(
                recurrenceId = "rent-1",
                startDate = LocalDate.of(2026, 1, 31),
                frequency = RecurrenceFrequency.MONTHLY,
                interval = 1,
                horizonMonths = 2,
                amountCents = 200000L,
            )

        assertEquals(first, second)
        assertEquals(first.map { it.key }, second.map { it.key })
    }

    @Test
    fun `monthly recurrence clamps end of month dates to the last valid day`() {
        val occurrences =
            RecurrenceEngine.generate(
                recurrenceId = "rent-2",
                startDate = LocalDate.of(2026, 1, 31),
                frequency = RecurrenceFrequency.MONTHLY,
                interval = 1,
                horizonMonths = 3,
                amountCents = 150000L,
            )

        assertEquals(
            listOf(
                LocalDate.of(2026, 1, 31),
                LocalDate.of(2026, 2, 28),
                LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 4, 30),
            ),
            occurrences.map { it.date },
        )
    }

    @Test
    fun `weekly recurrence with interval 2 respects the cadence`() {
        val occurrences =
            RecurrenceEngine.generate(
                recurrenceId = "weekly-1",
                startDate = LocalDate.of(2026, 1, 1),
                frequency = RecurrenceFrequency.WEEKLY,
                interval = 2,
                horizonMonths = 2,
                amountCents = 10000L,
            )

        assertTrue(occurrences.size >= 4)
        assertEquals(LocalDate.of(2026, 1, 1), occurrences.first().date)
        assertEquals(
            listOf(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 15),
                LocalDate.of(2026, 1, 29),
                LocalDate.of(2026, 2, 12),
                LocalDate.of(2026, 2, 26),
            ),
            occurrences.take(5).map { it.date },
        )
    }

    @Test
    fun `yearly recurrence keeps leap day valid and clamps to february last day`() {
        val occurrences =
            RecurrenceEngine.generate(
                recurrenceId = "yearly-leap",
                startDate = LocalDate.of(2024, 2, 29),
                frequency = RecurrenceFrequency.YEARLY,
                interval = 1,
                horizonMonths = 24,
                amountCents = 300000L,
            )

        assertEquals(
            listOf(
                LocalDate.of(2024, 2, 29),
                LocalDate.of(2025, 2, 28),
                LocalDate.of(2026, 2, 28),
            ),
            occurrences.take(3).map { it.date },
        )
    }

    @Test
    fun `generate stops after max occurrences even when horizon is longer`() {
        val occurrences =
            RecurrenceEngine.generate(
                recurrenceId = "cap-1",
                startDate = LocalDate.of(2026, 1, 10),
                frequency = RecurrenceFrequency.MONTHLY,
                interval = 1,
                horizonMonths = 12,
                maxOccurrences = 3,
                amountCents = 90000L,
            )

        assertEquals(
            listOf(
                LocalDate.of(2026, 1, 10),
                LocalDate.of(2026, 2, 10),
                LocalDate.of(2026, 3, 10),
            ),
            occurrences.map { it.date },
        )
    }
}
