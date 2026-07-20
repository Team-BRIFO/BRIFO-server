package com.brifo.server.stock.dto.request

import java.util.UUID

data class UpdateStockInterestsRequest(
    val stockIds: List<UUID>,
)
