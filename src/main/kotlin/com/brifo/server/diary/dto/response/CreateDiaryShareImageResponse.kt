package com.brifo.server.diary.dto.response

import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class CreateDiaryShareImageResponse(
    val diaryId: UUID,
    val shareImageUrl: String,
    val reused: Boolean,
    val changeRate: BigDecimal,
    val tradeDate: LocalDate,
    val apDelta: Int = 0,
)
