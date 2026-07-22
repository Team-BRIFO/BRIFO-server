package com.brifo.server.notification.repository

import com.brifo.server.agent.entity.AgentType
import com.brifo.server.briefing.entity.BriefingDirection
import com.brifo.server.decision.entity.DecisionDirection
import java.math.BigDecimal

object NotificationContentProjection {
    data class DecisionResult(
        val stockName: String,
        val changeRate: BigDecimal,
        val direction: DecisionDirection,
        val isCorrect: Boolean,
        val apAmount: Int,
        val balanceAp: Int,
    )

    data class BriefingReady(
        val agentType: AgentType,
        val stockName: String,
        val direction: BriefingDirection,
        val confidenceRate: Short,
    )

    data class NewsCard(
        val stockName: String,
    )

    data class AgentSalary(
        val agentType: AgentType,
        val salaryAmount: Int,
        val balanceAp: Int,
    )
}
