package com.brifo.server.user.dto.response

import java.util.UUID

data class GetMyPageResponse(
    val nickname: String,
    val companyName: String,
    val balanceAp: Int,
    val thisWeekEarnedAp: Int,
    val decisionAccuracyRate: Int,
    val totalDecision: Int,
    val consecutiveDays: Int,
    val learnedTermCount: Int,
    val stocks: List<MyPageStock>,
) {
    data class MyPageStock(
        val stockId: UUID,
        val name: String,
    )
}
