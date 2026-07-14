package com.brifo.server.diary.dto.response

import java.util.UUID

data class CreateDiaryShareImageResponse(
    val diaryId: UUID,
    val shareImageUrl: String,
    val reused: Boolean,
)
