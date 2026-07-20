package com.brifo.server.diary.dto.response

import com.brifo.server.agent.entity.AgentType
import com.brifo.server.briefing.entity.BriefingDirection
import java.math.BigDecimal
import java.util.UUID

data class GetDiaryDetailResponse(
    val diaryId: UUID,
    val shareImageUrl: String?,
    val stock: Stock,
    val agent: Agent,
    val briefing: Briefing,
    val decision: Decision,
) {
    data class Stock(
        val stockId: UUID,
        val name: String,
        val changeRate: BigDecimal,
    )

    data class Agent(
        val agentId: UUID,
        val agentType: AgentType,
        val nickname: String,
    )

    data class Briefing(
        val briefingId: UUID,
        val direction: BriefingDirection,
        val confidenceRate: Int,
    )

    data class Decision(
        val isCorrect: Boolean,
        val confidenceLevel: Int,
    )
}
