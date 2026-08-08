package com.brifo.server.decision

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

object DecisionMarketPolicy {
    val REGISTRATION_CUTOFF: LocalTime = LocalTime.of(15, 30)

    fun isRegistrationOpen(requestedAt: LocalDateTime): Boolean =
        requestedAt.toLocalTime().isBefore(REGISTRATION_CUTOFF)

    fun settlementCutoff(targetDate: LocalDate): LocalDateTime =
        targetDate.atTime(REGISTRATION_CUTOFF)
}
