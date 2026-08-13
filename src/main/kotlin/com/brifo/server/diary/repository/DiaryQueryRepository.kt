package com.brifo.server.diary.repository

import com.brifo.server.agent.entity.AgentType
import com.brifo.server.briefing.entity.BriefingDirection
import com.brifo.server.decision.entity.DecisionDirection
import com.querydsl.core.annotations.QueryProjection
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

interface DiaryQueryRepository {
    fun findDiaryPage(
        userPublicId: UUID,
        cursor: UUID?,
        limit: Long,
    ): List<DiaryListRow>

    fun findDiaryDetail(
        userPublicId: UUID,
        diaryPublicId: UUID,
    ): DiaryDetailRow?

    fun findCalendarRows(
        userPublicId: UUID,
        from: LocalDateTime,
        until: LocalDateTime,
    ): List<DiaryCalendarRow>

    fun findStatsRows(userPublicId: UUID): List<DiaryStatsRow>
}

@QueryProjection
data class DiaryListRow(
    val diaryId: UUID,
    val stockId: UUID,
    val stockName: String,
    val price: BigDecimal,
    val changeRate: BigDecimal,
    val tradeDate: LocalDate,
    val logoUrl: String?,
    val direction: DecisionDirection,
    val apDelta: Int,
    val isCorrect: Boolean,
)

@QueryProjection
data class DiaryDetailRow(
    val diaryId: UUID,
    val shareImageUrl: String?,
    val stockId: UUID,
    val stockName: String,
    val changeRate: BigDecimal,
    val tradeDate: LocalDate,
    val agentId: UUID,
    val agentType: AgentType,
    val agentNickname: String,
    val briefingId: UUID,
    val briefingDirection: BriefingDirection,
    val briefingConfidenceRate: Short,
    val isCorrect: Boolean,
    val confidenceLevel: Short,
)

@QueryProjection
data class DiaryCalendarRow(
    val decidedAt: LocalDateTime,
    val direction: DecisionDirection,
    val isCorrect: Boolean,
)

@QueryProjection
data class DiaryStatsRow(
    val diaryId: UUID,
    val settledAt: LocalDateTime,
    val isCorrect: Boolean,
    val direction: DecisionDirection,
    val confidenceLevel: Short,
    val agentId: UUID,
    val stockId: UUID,
    val stockName: String,
)
