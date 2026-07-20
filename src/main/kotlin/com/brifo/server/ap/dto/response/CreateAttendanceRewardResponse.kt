package com.brifo.server.ap.dto.response

data class CreateAttendanceRewardResponse(
    val rewardedAp: Int,
    val bonusRewarded: Boolean,
    val consecutiveDays: Int,
    val balanceAp: Int,
)
