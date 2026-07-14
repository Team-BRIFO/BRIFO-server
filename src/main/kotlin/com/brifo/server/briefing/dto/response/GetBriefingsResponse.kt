package com.brifo.server.briefing.dto.response

import com.brifo.server.briefing.entity.BriefingStatus
import java.util.UUID

data class GetBriefingsResponse(
    val status: Status,
    val items: List<Item>,
) {
    enum class Status {
        ALL,
        COMPLETED,
    }

    data class Item(
        val cardId: UUID,
        val briefingStatus: BriefingStatus,
        val stock: BriefingStockResponse,
        val directionCounts: BriefingDirectionCountsResponse,
    )
}
