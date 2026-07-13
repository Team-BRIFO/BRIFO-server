package com.brifo.server.diary.dto.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class GetDiaryCalendarRequest(
    @field:Min(2000)
    val year: Int,
    @field:Min(1)
    @field:Max(12)
    val month: Int,
)
