package com.brifo.server.badge.dto.response

import java.util.UUID

data class GetOwnedBadgeResponse(
    val badgeId: UUID,
    val code: String,
    val name: String,
    val description: String?,
    val rewardAp: Int,
)
