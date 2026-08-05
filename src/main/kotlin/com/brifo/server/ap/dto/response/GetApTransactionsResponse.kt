package com.brifo.server.ap.dto.response

import com.brifo.server.ap.entity.ApTransactionReason
import com.brifo.server.global.common.CursorPage
import java.time.LocalDateTime
import java.util.UUID

data class GetApTransactionsResponse(
    val summary: ApSummary,
    val page: CursorPage<ApTransactionItem>,
) {
    data class ApSummary(
        val balanceAp: Int,
        val monthlyEarnedAp: Int,
        val monthlyLostAp: Int,
    )

    data class MonthlyAmounts(
        val earnedAp: Int,
        val lostAp: Int,
    )

    data class ApTransactionItem(
        val apTransactionId: UUID,
        val reason: ApTransactionReason,
        val amount: Int,
        val createdAt: LocalDateTime,
    )
}
