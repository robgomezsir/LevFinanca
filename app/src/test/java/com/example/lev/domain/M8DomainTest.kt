package com.example.lev.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class M8DomainTest {
    @Test
    fun `lock settings keep the app hidden after the configured timeout`() {
        val policy = SecurityPolicy(lockAfterMinutes = 5, biometricEnabled = true, hideSensitiveValues = true)

        assertEquals(5L * 60L, policy.lockAfterSeconds)
        assertTrue(policy.shouldLockAfterBackground(300L))
        assertFalse(policy.shouldLockAfterBackground(180L))
    }

    @Test
    fun `sensitive values are masked before being displayed on lock screen`() {
        val masked = SecurityPolicy.maskForLockScreen("R$ 1.234,56")

        assertEquals("R$ •••••••", masked)
    }

    @Test
    fun `notification planner schedules reminders based on due date and lead days`() {
        val dueDate = LocalDate.of(2026, 10, 20)

        val reminders =
            NotificationPlanner.scheduleReminders(
                eventDate = dueDate,
                leadDays = 3,
                remindOnEventDay = true,
                currentDate = LocalDate.of(2026, 10, 15),
            )

        assertEquals(
            listOf(
                LocalDate.of(2026, 10, 17),
                LocalDate.of(2026, 10, 20),
            ),
            reminders,
        )
    }

    @Test
    fun `csv import normalizes brazilian currency and removes duplicates`() {
        val rows =
            listOf(
                ImportRow("1", "SABESP", "R$ 100,00", "2026-10-05", "Despesa"),
                ImportRow("2", "SABESP", "100,00", "05/10/2026", "Despesa"),
                ImportRow("3", "Mercado", "R$ 35,60", "2026-10-03", "Despesa"),
            )

        val normalized = CsvImportParser.normalize(rows)
        val duplicateKey = CsvImportParser.duplicateKey("SABESP", "100,00", "05/10/2026")

        assertEquals(2, normalized.size)
        assertEquals("sabesp|10000|2026-10-05", duplicateKey)
    }
}
