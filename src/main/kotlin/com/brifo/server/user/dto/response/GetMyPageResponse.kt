package com.brifo.server.user.dto.response

data class GetMyPageResponse(
    val nickname: String,
    val companyName: String,
    val balanceAp: Int,
    val thisWeekEarnedAp: Int,
    val decisionAccuracyRate: Int,
    val totalDecision: Int,
    val consecutiveDays: Int,
    val learnedTermCount: Int,
)
