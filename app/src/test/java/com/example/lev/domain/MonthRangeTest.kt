package com.example.lev.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class MonthRangeTest {
    @Test
    fun `standard month start 1st`() {
        val ref = LocalDate.of(2026, 10, 15)
        val (start, end) = MonthRange.calculate(ref, 1)
        assertEquals(LocalDate.of(2026, 10, 1), start)
        assertEquals(LocalDate.of(2026, 10, 31), end)
    }

    @Test
    fun `custom start day 5th, ref after 5th`() {
        val ref = LocalDate.of(2026, 10, 15)
        val (start, end) = MonthRange.calculate(ref, 5)
        assertEquals(LocalDate.of(2026, 10, 5), start)
        assertEquals(LocalDate.of(2026, 11, 4), end)
    }

    @Test
    fun `custom start day 5th, ref before 5th`() {
        val ref = LocalDate.of(2026, 10, 2)
        val (start, end) = MonthRange.calculate(ref, 5)
        assertEquals(LocalDate.of(2026, 9, 5), start)
        assertEquals(LocalDate.of(2026, 10, 4), end)
    }
}
