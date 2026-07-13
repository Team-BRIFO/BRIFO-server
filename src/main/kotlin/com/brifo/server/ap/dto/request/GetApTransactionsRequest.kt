package com.brifo.server.ap.dto.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.util.UUID

data class GetApTransactionsRequest(
    val cursor: UUID? = null,
    @field:Min(1)
    @field:Max(50)
    val size: Int = 20,
)
