package com.brifo.server.diary.dto.response

import com.brifo.server.agent.entity.AgentType
import com.brifo.server.briefing.entity.BriefingDirection
import java.math.BigDecimal
import java.util.UUID

data class GetDiaryDetailResponse(
    val diaryId: UUID,
    val shareImageUrl: String?,
    val stock: DiaryDetailStock,
    val agent: DiaryDetailAgent,
    val briefing: DiaryDetailBriefing,
    val decision: DiaryDetailDecision,
) {
    data class DiaryDetailStock(
        val stockId: UUID,
        val name: String,
        val changeRate: BigDecimal,
    )

    data class DiaryDetailAgent(
        val agentId: UUID,
        val agentType: AgentType,
        val nickname: String,
    )

    data class DiaryDetailBriefing(
        val briefingId: UUID,
        val direction: BriefingDirection,
        val confidenceRate: Int,
    )

    data class DiaryDetailDecision(
        val isCorrect: Boolean,
        val confidenceLevel: Int,
    )
}
