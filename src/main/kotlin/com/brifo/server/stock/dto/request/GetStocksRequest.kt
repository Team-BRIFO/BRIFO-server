package com.brifo.server.stock.dto.request

import java.util.UUID

data class GetStocksRequest(
    val keyword: String? = null,
    val cursor: UUID? = null,
    val size: Int = 20,
)
