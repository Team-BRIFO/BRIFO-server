package com.brifo.server.decision

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

object DecisionMarketPolicy {
    val REGISTRATION_CUTOFF: LocalTime = LocalTime.of(15, 30)

    fun isRegistrationOpen(requestedAt: LocalDateTime): Boolean =
        requestedAt.dayOfWeek !in CLOSED_DAYS && requestedAt.toLocalTime().isBefore(REGISTRATION_CUTOFF)

    fun isBusinessDay(requestedAt: LocalDateTime): Boolean =
        isMarketOpenOn(requestedAt.toLocalDate())

    /** 해당 날짜에 장이 열리는지. 공휴일은 반영하지 않는다. */
    fun isMarketOpenOn(date: LocalDate): Boolean = date.dayOfWeek !in CLOSED_DAYS

    fun settlementCutoff(targetDate: LocalDate): LocalDateTime =
        targetDate.atTime(REGISTRATION_CUTOFF)

    private val CLOSED_DAYS = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
}
