package com.brifo.server.diary.dto.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.util.UUID

data class GetDiariesRequest(
    val cursor: UUID? = null,
    @field:Min(1)
    @field:Max(50)
    val size: Int = 20,
)
