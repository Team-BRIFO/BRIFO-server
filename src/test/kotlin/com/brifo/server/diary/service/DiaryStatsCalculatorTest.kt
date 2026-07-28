package com.brifo.server.diary.service

import com.brifo.server.agent.entity.AgentType
import com.brifo.server.decision.entity.DecisionDirection
import com.brifo.server.diary.dto.response.GetDiaryStatsResponse
import com.brifo.server.diary.repository.DiaryStatsRow
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import kotlin.test.assertEquals

class DiaryStatsCalculatorTest {
    private val calculator = DiaryStatsCalculator(
        Clock.fixed(Instant.parse("2026-07-23T03:00:00Z"), ZoneId.of("Asia/Seoul")),
    )
    private val agent = DiaryAgentSummary(UUID.randomUUID(), AgentType.ROOKIE, "루키")

    @Test
    fun `결정이 없어도 고정 분류와 보유 사원은 0으로 반환한다`() {
        val result = calculator.calculate(emptyList(), listOf(agent))

        assertEquals(0, result.summary.settledDecisionCount)
        assertEquals("0.0", result.summary.averageConfidenceLevel.toPlainString())
        assertEquals(DecisionDirection.entries.toSet(), result.directionStats.map { it.direction }.toSet())
        assertEquals(GetDiaryStatsResponse.Level.entries, result.confidenceLevelStats.map { it.level })
        assertEquals(0, result.agentStats.single().settledDecisionCount)
        assertEquals(emptyList(), result.stockStats)
    }

    @Test
    fun `최근 30일은 시작일 자정과 오늘을 포함하고 이전 데이터는 제외한다`() {
        val rows = listOf(
            row(LocalDateTime.of(2026, 6, 23, 23, 59), true),
            row(LocalDateTime.of(2026, 6, 24, 0, 0), true),
            row(LocalDateTime.of(2026, 7, 23, 23, 59), false),
        )

        val summary = calculator.calculate(rows, listOf(agent)).summary

        assertEquals(2, summary.recent30DaysSettledDecisionCount)
        assertEquals(1, summary.recent30DaysCorrectDecisionCount)
        assertEquals(50, summary.recent30DaysAccuracyRate)
    }

    @Test
    fun `연속 적중은 오답에서 끊기고 최대 길이를 반환한다`() {
        val rows = listOf(true, true, false, true).mapIndexed { index, correct ->
            row(LocalDateTime.of(2026, 7, index + 1, 10, 0), correct)
        }

        val summary = calculator.calculate(rows, listOf(agent)).summary

        assertEquals(2, summary.bestCorrectStreak)
    }

    @Test
    fun `사원 통계는 각 사원이 채택된 결정만 집계한다`() {
        val otherAgent = DiaryAgentSummary(UUID.randomUUID(), AgentType.PRO, "프로")
        val rows = listOf(
            row(isCorrect = true),
            row(isCorrect = false),
            row(isCorrect = true, agentId = otherAgent.agentId),
        )

        val stats = calculator.calculate(rows, listOf(agent, otherAgent)).agentStats

        assertEquals(listOf(2, 1), stats.map { it.settledDecisionCount })
        assertEquals(listOf(1, 1), stats.map { it.correctDecisionCount })
        assertEquals(listOf(50, 100), stats.map { it.accuracyRate })
    }

    @Test
    fun `확신도 평균과 적중률은 합의한 자릿수로 반올림한다`() {
        val rows = listOf(
            row(LocalDateTime.of(2026, 7, 1, 10, 0), true, confidenceLevel = 2),
            row(LocalDateTime.of(2026, 7, 2, 10, 0), true, confidenceLevel = 4),
            row(LocalDateTime.of(2026, 7, 3, 10, 0), false, confidenceLevel = 5),
        )

        val result = calculator.calculate(rows, listOf(agent))

        assertEquals("3.7", result.summary.averageConfidenceLevel.toPlainString())
        assertEquals(67, result.summary.recent30DaysAccuracyRate)
        assertEquals(listOf(1, 0, 2), result.confidenceLevelStats.map { it.settledDecisionCount })
    }

    @Test
    fun `종목 통계는 적중률과 결정 수 우선순위로 세 개만 반환한다`() {
        val rows = listOf(
            row(stockName = "다", isCorrect = true),
            row(stockName = "가", isCorrect = true),
            row(stockName = "가", isCorrect = false),
            row(stockName = "나", isCorrect = true),
            row(stockName = "라", isCorrect = false),
        )

        val stockStats = calculator.calculate(rows, listOf(agent)).stockStats

        assertEquals(listOf("나", "다", "가"), stockStats.map { it.name })
    }

    private fun row(
        settledAt: LocalDateTime = LocalDateTime.of(2026, 7, 1, 10, 0),
        isCorrect: Boolean,
        confidenceLevel: Short = 3,
        stockName: String = "삼성전자",
        agentId: UUID = agent.agentId,
    ): DiaryStatsRow =
        DiaryStatsRow(
            diaryId = UUID.randomUUID(),
            settledAt = settledAt,
            isCorrect = isCorrect,
            direction = DecisionDirection.UP,
            confidenceLevel = confidenceLevel,
            agentId = agentId,
            stockId = UUID.nameUUIDFromBytes(stockName.toByteArray()),
            stockName = stockName,
        )
}
