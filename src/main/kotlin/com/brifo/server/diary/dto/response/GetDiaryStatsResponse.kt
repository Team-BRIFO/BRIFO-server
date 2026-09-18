package com.brifo.server.diary.dto.response

import com.brifo.server.agent.entity.AgentType
import com.brifo.server.decision.entity.DecisionDirection
import java.math.BigDecimal
import java.util.UUID

data class GetDiaryStatsResponse(
    val summary: DiaryStatsSummary,
    val directionStats: List<DirectionStat>,
    val agentStats: List<AgentStat>,
    val allocationRateStats: List<AllocationRateStat>,
    val stockStats: List<StockStat>,
) {
    data class DiaryStatsSummary(
        val recent30DaysSettledDecisionCount: Int,
        val recent30DaysCorrectDecisionCount: Int,
        val recent30DaysAccuracyRate: Int,
        val settledDecisionCount: Int,
        val correctDecisionCount: Int,
        val averageAllocationRatePercent: BigDecimal,
        val bestCorrectStreak: Int,
    )

    data class DirectionStat(
        val direction: DecisionDirection,
        val settledDecisionCount: Int,
        val correctDecisionCount: Int,
        val accuracyRate: Int,
    )

    data class AgentStat(
        val agentId: UUID,
        val agentType: AgentType,
        val nickname: String,
        val settledDecisionCount: Int,
        val correctDecisionCount: Int,
        val accuracyRate: Int,
    )

    data class AllocationRateStat(
        val level: Level,
        val settledDecisionCount: Int,
        val correctDecisionCount: Int,
        val accuracyRate: Int,
    )

    data class StockStat(
        val stockId: UUID,
        val name: String,
        val settledDecisionCount: Int,
        val correctDecisionCount: Int,
        val accuracyRate: Int,
    )

    enum class Level {
        LOW,
        MEDIUM,
        HIGH,
    }
}
