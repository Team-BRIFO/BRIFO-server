package com.brifo.server.diary.service

import com.brifo.server.agent.repository.AgentRepository
import com.brifo.server.decision.entity.DecisionDirection
import com.brifo.server.diary.dto.request.GetDiariesRequest
import com.brifo.server.diary.dto.request.GetDiaryCalendarRequest
import com.brifo.server.diary.dto.response.GetDiariesResponse
import com.brifo.server.diary.dto.response.GetDiaryCalendarResponse
import com.brifo.server.diary.dto.response.GetDiaryDetailResponse
import com.brifo.server.diary.dto.response.GetDiaryStatsResponse
import com.brifo.server.diary.exception.DiaryNotFoundException
import com.brifo.server.diary.repository.DiaryEntryRepository
import com.brifo.server.global.common.CursorPage
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.UUID

@Service
class DiaryService(
    private val diaryEntryRepository: DiaryEntryRepository,
    private val agentRepository: AgentRepository,
    private val statsCalculator: DiaryStatsCalculator,
) {
    @Transactional(readOnly = true)
    fun getDiaries(
        userPublicId: UUID,
        request: GetDiariesRequest,
    ): GetDiariesResponse {
        val rows = diaryEntryRepository.findDiaryPage(
            userPublicId = userPublicId,
            cursor = request.cursor,
            limit = request.size.toLong() + 1,
        )
        val hasNext = rows.size > request.size
        val items = rows.take(request.size).map { row ->
            GetDiariesResponse.Item(
                diaryId = row.diaryId,
                stock = GetDiariesResponse.Stock(row.stockId, row.stockName),
                decision = GetDiariesResponse.Decision(row.direction, row.apDelta, row.isCorrect),
            )
        }

        return GetDiariesResponse(
            page = CursorPage(
                items = items,
                nextCursor = items.lastOrNull()?.diaryId?.takeIf { hasNext },
                hasNext = hasNext,
            ),
        )
    }

    @Transactional(readOnly = true)
    fun getDiaryDetail(
        userPublicId: UUID,
        diaryPublicId: UUID,
    ): GetDiaryDetailResponse {
        val row = diaryEntryRepository.findDiaryDetail(userPublicId, diaryPublicId)
            ?: throw DiaryNotFoundException()

        return GetDiaryDetailResponse(
            diaryId = row.diaryId,
            shareImageUrl = row.shareImageUrl,
            stock = GetDiaryDetailResponse.Stock(
                stockId = row.stockId,
                name = row.stockName,
                changeRate = row.changeRate.setScale(0, RoundingMode.HALF_UP).intValueExact(),
            ),
            agent = GetDiaryDetailResponse.Agent(row.agentId, row.agentType, row.agentNickname),
            briefing = GetDiaryDetailResponse.Briefing(
                row.briefingId,
                row.briefingDirection,
                row.briefingConfidenceRate.toInt(),
            ),
            decision = GetDiaryDetailResponse.Decision(row.isCorrect, row.confidenceLevel.toInt()),
        )
    }

    @Transactional(readOnly = true)
    fun getDiaryCalendar(
        userPublicId: UUID,
        request: GetDiaryCalendarRequest,
    ): GetDiaryCalendarResponse {
        val firstDay = LocalDate.of(request.year, request.month, 1)
        val rows = diaryEntryRepository.findCalendarRows(
            userPublicId = userPublicId,
            from = firstDay.atStartOfDay(),
            until = firstDay.plusMonths(1).atStartOfDay(),
        )
        val correctCount = rows.count { it.isCorrect }
        val days = rows.groupBy { it.decidedAt.toLocalDate() }.map { (date, dateRows) ->
            GetDiaryCalendarResponse.Day(
                date = date,
                direction = GetDiaryCalendarResponse.Direction(
                    up = dateRows.any { it.direction == DecisionDirection.UP },
                    down = dateRows.any { it.direction == DecisionDirection.DOWN },
                    neutral = dateRows.any { it.direction == DecisionDirection.NEUTRAL },
                ),
            )
        }

        return GetDiaryCalendarResponse(
            year = request.year,
            month = request.month,
            settledDecisionCount = rows.size,
            correctDecisionCount = correctCount,
            accuracyRate = accuracyRate(correctCount, rows.size),
            days = days,
        )
    }

    @Transactional(readOnly = true)
    fun getDiaryStats(userPublicId: UUID): GetDiaryStatsResponse {
        val rows = diaryEntryRepository.findStatsRows(userPublicId)
        val agents = agentRepository.findAllByUserPublicIdOrderByAgentTypeAsc(userPublicId).map { agent ->
            DiaryAgentSummary(
                agentId = requireNotNull(agent.publicId),
                agentType = agent.agentType,
                nickname = agent.nickname,
            )
        }
        return statsCalculator.calculate(
            rows = rows,
            agents = agents,
        )
    }

    private fun accuracyRate(
        correct: Int,
        settled: Int,
    ): Int =
        if (settled == 0) {
            0
        } else {
            correct.toBigDecimal()
                .multiply(BigDecimal.valueOf(100))
                .divide(settled.toBigDecimal(), 0, RoundingMode.HALF_UP)
                .intValueExact()
        }

}
