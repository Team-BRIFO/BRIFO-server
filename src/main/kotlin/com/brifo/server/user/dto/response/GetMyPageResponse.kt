package com.brifo.server.user.dto.response

import io.swagger.v3.oas.annotations.media.ArraySchema
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
    @field:ArraySchema(minItems = 1, maxItems = 3)
    val stocks: List<MyPageStock>,
) {
    data class MyPageStock(
        val stockId: UUID,
        val name: String,
        val logoUrl: String?,
    )
}
