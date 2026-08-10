package com.brifo.server.diary.share

import com.brifo.server.agent.entity.AgentType
import com.brifo.server.briefing.entity.BriefingDirection
import java.math.BigDecimal
import java.util.UUID

data class DiaryShareImageModel(
    val diaryId: UUID,
    val stockName: String,
    val changeRate: BigDecimal,
    val agentType: AgentType,
    val agentNickname: String,
    val briefingDirection: BriefingDirection,
    val briefingConfidenceRate: Int,
    val isCorrect: Boolean,
    val decisionConfidenceLevel: Int,
)

data class ShareImageFile(
    val bytes: ByteArray,
    val contentType: String,
    val extension: String,
)
