package com.brifo.server.diary.service

import com.brifo.server.agent.entity.AgentType
import com.brifo.server.decision.entity.DecisionDirection
import com.brifo.server.diary.dto.response.GetDiaryStatsResponse
import com.brifo.server.diary.repository.DiaryStatsRow
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

@Component
class DiaryStatsCalculator(
    private val clock: Clock,
) {
    fun calculate(
        rows: List<DiaryStatsRow>,
        agents: List<DiaryAgentSummary>,
    ): GetDiaryStatsResponse {
        val correctCount = rows.count { it.isCorrect }
        val today = LocalDate.now(clock)
        val recentFrom = today.minusDays(29).atStartOfDay()
        val recentUntil = today.plusDays(1).atStartOfDay()
        val recentRows = rows.filter { it.settledAt in recentFrom..<recentUntil }
        val recentCorrectCount = recentRows.count { it.isCorrect }

        return GetDiaryStatsResponse(
            summary = GetDiaryStatsResponse.Summary(
                recent30DaysSettledDecisionCount = recentRows.size,
                recent30DaysCorrectDecisionCount = recentCorrectCount,
                recent30DaysAccuracyRate = accuracyRate(recentCorrectCount, recentRows.size),
                settledDecisionCount = rows.size,
                correctDecisionCount = correctCount,
                averageConfidenceLevel = averageConfidenceLevel(rows),
                bestCorrectStreak = bestCorrectStreak(rows),
            ),
            directionStats = directionStats(rows),
            agentStats = agentStats(rows, agents),
            confidenceLevelStats = confidenceLevelStats(rows),
            stockStats = stockStats(rows),
        )
    }

    private fun directionStats(rows: List<DiaryStatsRow>): List<GetDiaryStatsResponse.DirectionStat> =
        listOf(DecisionDirection.UP, DecisionDirection.NEUTRAL, DecisionDirection.DOWN).map { direction ->
            val group = rows.filter { it.direction == direction }
            val correct = group.count { it.isCorrect }
            GetDiaryStatsResponse.DirectionStat(direction, group.size, correct, accuracyRate(correct, group.size))
        }

    private fun agentStats(
        rows: List<DiaryStatsRow>,
        agents: List<DiaryAgentSummary>,
    ): List<GetDiaryStatsResponse.AgentStat> =
        agents.map { agent ->
            val group = rows.filter { it.agentId == agent.agentId }
            val correct = group.count { it.isCorrect }
            GetDiaryStatsResponse.AgentStat(
                agent.agentId,
                agent.agentType,
                agent.nickname,
                group.size,
                correct,
                accuracyRate(correct, group.size),
            )
        }

    private fun confidenceLevelStats(rows: List<DiaryStatsRow>): List<GetDiaryStatsResponse.ConfidenceLevelStat> =
        GetDiaryStatsResponse.Level.entries.map { level ->
            val group = rows.filter { confidenceLevel(it.confidenceLevel.toInt()) == level }
            val correct = group.count { it.isCorrect }
            GetDiaryStatsResponse.ConfidenceLevelStat(level, group.size, correct, accuracyRate(correct, group.size))
        }

    private fun stockStats(rows: List<DiaryStatsRow>): List<GetDiaryStatsResponse.StockStat> =
        rows.groupBy { it.stockId }.values.map { group ->
            val correct = group.count { it.isCorrect }
            GetDiaryStatsResponse.StockStat(
                stockId = group.first().stockId,
                name = group.first().stockName,
                settledDecisionCount = group.size,
                correctDecisionCount = correct,
                accuracyRate = accuracyRate(correct, group.size),
            )
        }.sortedWith(
            compareByDescending<GetDiaryStatsResponse.StockStat> { it.accuracyRate }
                .thenByDescending { it.settledDecisionCount }
                .thenBy { it.name },
        ).take(3)

    private fun averageConfidenceLevel(rows: List<DiaryStatsRow>): BigDecimal =
        if (rows.isEmpty()) {
            BigDecimal.ZERO.setScale(1)
        } else {
            rows.sumOf { it.confidenceLevel.toInt() }.toBigDecimal()
                .divide(rows.size.toBigDecimal(), 1, RoundingMode.HALF_UP)
        }

    private fun bestCorrectStreak(rows: List<DiaryStatsRow>): Int {
        var current = 0
        var best = 0
        rows.forEach { row ->
            current = if (row.isCorrect) current + 1 else 0
            best = maxOf(best, current)
        }
        return best
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

    private fun confidenceLevel(level: Int): GetDiaryStatsResponse.Level =
        when (level) {
            in 1..2 -> GetDiaryStatsResponse.Level.LOW
            3 -> GetDiaryStatsResponse.Level.MEDIUM
            else -> GetDiaryStatsResponse.Level.HIGH
        }
}

data class DiaryAgentSummary(
    val agentId: UUID,
    val agentType: AgentType,
    val nickname: String,
)
