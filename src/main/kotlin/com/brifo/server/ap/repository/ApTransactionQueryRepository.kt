package com.brifo.server.ap.repository

import com.brifo.server.ap.dto.response.GetApTransactionsResponse
import java.time.LocalDateTime
import java.util.UUID

interface ApTransactionQueryRepository {
    fun sumBriefingSalaryBalance(briefingId: Long): Int

    fun findPageByUserId(
        userId: Long,
        cursor: UUID?,
        limit: Int,
    ): List<GetApTransactionsResponse.Item>

    fun findMonthlyAmountsByUserId(
        userId: Long,
        monthStart: LocalDateTime,
        nextMonthStart: LocalDateTime,
    ): GetApTransactionsResponse.MonthlyAmounts
}
