package com.example.lev.domain

import java.time.LocalDate

object NotificationPlanner {
    fun scheduleReminders(
        eventDate: LocalDate,
        leadDays: Int,
        remindOnEventDay: Boolean,
        currentDate: LocalDate,
    ): List<LocalDate> {
        val reminders = mutableListOf<LocalDate>()
        val firstReminder = eventDate.minusDays(leadDays.toLong())

        if (!firstReminder.isBefore(currentDate)) {
            reminders += firstReminder
        }
        if (remindOnEventDay && !eventDate.isBefore(currentDate)) {
            reminders += eventDate
        }

        return reminders
    }
}
