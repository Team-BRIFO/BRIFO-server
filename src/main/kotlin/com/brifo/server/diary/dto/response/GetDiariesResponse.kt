package com.brifo.server.diary.dto.response

import com.brifo.server.decision.entity.DecisionDirection
import com.brifo.server.global.common.CursorPage
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class GetDiariesResponse(
    val page: CursorPage<Item>,
) {
    data class Item(
        val diaryId: UUID,
        val stock: Stock,
        val decision: Decision,
    )

    data class Stock(
        val stockId: UUID,
        val name: String,
        val price: Long,
        val changeRate: BigDecimal,
        val tradeDate: LocalDate,
        val logoUrl: String?,
    )

    data class Decision(
        val direction: DecisionDirection,
        val apDelta: Int,
        val isCorrect: Boolean,
    )
}
