package com.brifo.server.diary.dto.response

import java.time.LocalDate

data class GetDiaryCalendarResponse(
    val year: Int,
    val month: Int,
    val settledDecisionCount: Int,
    val correctDecisionCount: Int,
    val accuracyRate: Int,
    val days: List<Day>,
) {
    data class Day(
        val date: LocalDate,
        val direction: Direction,
    )

    data class Direction(
        val up: Boolean,
        val down: Boolean,
        val neutral: Boolean,
    )
}
