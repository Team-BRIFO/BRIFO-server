package com.brifo.server.stock.dto.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.util.UUID

data class GetStocksRequest(
    val keyword: String? = null,
    val cursor: UUID? = null,
    @field:Min(1)
    @field:Max(50)
    val size: Int = 20,
)
