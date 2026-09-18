package com.brifo.server.diary.dto.response

import com.brifo.server.agent.entity.AgentType
import com.brifo.server.decision.entity.DecisionDirection
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class GetDiaryDayDetailResponse(
    val date: LocalDate,
    val items: List<DayDetailItem>,
) {
    data class DayDetailItem(
        val diaryId: UUID,
        val stock: DayDetailStock,
        val agent: DayDetailAgent,
        val decision: DayDetailDecision,
    )

    data class DayDetailStock(
        val stockId: UUID,
        val name: String,
        val logoUrl: String?,
        val changeRate: BigDecimal,
    )

    data class DayDetailAgent(
        val agentId: UUID,
        val agentType: AgentType,
        val nickname: String,
    )

    data class DayDetailDecision(
        val direction: DecisionDirection,
        val confidenceLevel: Int,
        val isCorrect: Boolean,
        val apDelta: Int,
    )
}
