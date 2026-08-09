package com.brifo.server.ap.dto.response

import com.brifo.server.ap.entity.ApTransactionReason
import com.brifo.server.global.common.CursorPage
import com.fasterxml.jackson.annotation.JsonFormat
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
        @field:JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss[.SSSSSS]")
        val createdAt: LocalDateTime,
    )
}
