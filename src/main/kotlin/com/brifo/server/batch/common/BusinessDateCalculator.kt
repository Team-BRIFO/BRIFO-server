package com.brifo.server.batch.common

import org.springframework.stereotype.Component
import java.time.DayOfWeek
import java.time.LocalDate

@Component
class BusinessDateCalculator {
    fun isBusinessDay(date: LocalDate): Boolean =
        date.dayOfWeek != DayOfWeek.SATURDAY && date.dayOfWeek != DayOfWeek.SUNDAY

    fun previousBusinessDay(date: LocalDate): LocalDate {
        var candidate = date.minusDays(1)
        while (!isBusinessDay(candidate)) {
            candidate = candidate.minusDays(1)
        }
        return candidate
    }

    fun nextBusinessDay(date: LocalDate): LocalDate {
        var candidate = date.plusDays(1)
        while (!isBusinessDay(candidate)) {
            candidate = candidate.plusDays(1)
        }
        return candidate
    }
}
