package com.brifo.server.batch.collection

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

enum class CollectionRound(
    val cutoff: LocalTime,
    val recencyScore: BigDecimal,
) {
    MORNING(LocalTime.of(7, 0), BigDecimal("0.0")),
    MIDDAY(LocalTime.of(11, 30), BigDecimal("0.5")),
    CLOSING(LocalTime.of(15, 40), BigDecimal("1.0")),
    ;

    fun cutoffAt(targetDate: LocalDate): LocalDateTime = targetDate.atTime(cutoff)

    companion object {
        fun fromPublishedAt(publishedAt: LocalDateTime): CollectionRound =
            when {
                !publishedAt.toLocalTime().isAfter(MORNING.cutoff) -> MORNING
                !publishedAt.toLocalTime().isAfter(MIDDAY.cutoff) -> MIDDAY
                else -> CLOSING
            }
    }
}
