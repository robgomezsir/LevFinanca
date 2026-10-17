package com.example.lev.domain

import java.time.LocalDate

enum class RecurrenceFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
}

data class RecurrenceOccurrence(
    val recurrenceId: String,
    val date: LocalDate,
    val amountCents: Long?,
    val status: String = "PENDING",
    val isVariable: Boolean = false,
) {
    val key: String = "$recurrenceId:$date"
}

object RecurrenceEngine {
    fun generate(
        recurrenceId: String,
        startDate: LocalDate,
        frequency: RecurrenceFrequency,
        interval: Int = 1,
        horizonMonths: Int = 3,
        amountCents: Long? = null,
        endDate: LocalDate? = null,
        maxOccurrences: Int? = null,
        isVariable: Boolean = false,
    ): List<RecurrenceOccurrence> {
        require(interval > 0) { "interval must be greater than zero" }
        require(horizonMonths >= 0) { "horizonMonths must be greater than or equal to zero" }

        val limitDate = endDate ?: startDate.plusMonths(horizonMonths.toLong())
        val occurrences = mutableListOf<RecurrenceOccurrence>()
        var current = startDate
        var generated = 0

        while (!current.isAfter(limitDate) && (maxOccurrences == null || generated < maxOccurrences)) {
            occurrences +=
                RecurrenceOccurrence(
                    recurrenceId = recurrenceId,
                    date = current,
                    amountCents = amountCents,
                    status = "PENDING",
                    isVariable = isVariable,
                )
            generated += 1

            if (current == limitDate && endDate == null) break
            current = nextDate(current, frequency, interval)
        }

        if (endDate != null) {
            return occurrences.filter { !it.date.isAfter(endDate) }
        }

        return occurrences
    }

    private fun nextDate(
        date: LocalDate,
        frequency: RecurrenceFrequency,
        interval: Int,
    ): LocalDate {
        return when (frequency) {
            RecurrenceFrequency.DAILY -> date.plusDays(interval.toLong())
            RecurrenceFrequency.WEEKLY -> date.plusDays(7L * interval.toLong())
            RecurrenceFrequency.MONTHLY -> nextMonthlyDate(date, interval)
            RecurrenceFrequency.YEARLY -> nextYearlyDate(date, interval)
        }
    }

    private fun nextMonthlyDate(
        date: LocalDate,
        interval: Int,
    ): LocalDate {
        val nextMonth = date.plusMonths(interval.toLong())
        return clampToMonthEnd(date, nextMonth.year, nextMonth.monthValue)
    }

    private fun nextYearlyDate(
        date: LocalDate,
        interval: Int,
    ): LocalDate {
        val nextYear = date.plusYears(interval.toLong())
        return clampToMonthEnd(date, nextYear.year, nextYear.monthValue)
    }

    private fun clampToMonthEnd(
        anchor: LocalDate,
        year: Int,
        month: Int,
    ): LocalDate {
        val monthLength = LocalDate.of(year, month, 1).lengthOfMonth()
        val targetDay =
            if (anchor.dayOfMonth == anchor.lengthOfMonth()) {
                monthLength
            } else {
                minOf(anchor.dayOfMonth, monthLength)
            }
        return LocalDate.of(year, month, targetDay)
    }
}
