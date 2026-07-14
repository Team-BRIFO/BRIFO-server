package com.brifo.server.badge.dto.response

import java.util.UUID

data class GetBadgesResponse(
    val items: List<Item>,
) {
    data class Item(
        val badgeId: UUID,
        val code: String,
        val name: String,
        val isOwned: Boolean,
    )
}
