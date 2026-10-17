package com.example.lev.domain

import java.time.LocalDate

object MonthRange {
    /**
     * Calculates the start and end dates for a financial month based on a custom starting day.
     * E.g., if startingDay = 5 and referenceDate is 2026-10-10, the month goes from Oct 5 to Nov 4.
     */
    fun calculate(
        referenceDate: LocalDate,
        startingDay: Int,
    ): Pair<LocalDate, LocalDate> {
        val safeStartDay = startingDay.coerceIn(1, 28)

        val start: LocalDate
        if (referenceDate.dayOfMonth >= safeStartDay) {
            start = LocalDate.of(referenceDate.year, referenceDate.month, safeStartDay)
        } else {
            val prevMonth = referenceDate.minusMonths(1)
            start = LocalDate.of(prevMonth.year, prevMonth.month, safeStartDay)
        }

        val end = start.plusMonths(1).minusDays(1)
        return Pair(start, end)
    }
}
