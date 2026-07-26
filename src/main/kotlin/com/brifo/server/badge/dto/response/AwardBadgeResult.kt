package com.brifo.server.badge.dto.response

import java.util.UUID

data class AwardBadgeResult(
    val badgeId: UUID,
    val rewardAp: Int,
    val awarded: Boolean,
)
