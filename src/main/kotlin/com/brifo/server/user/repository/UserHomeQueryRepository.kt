package com.brifo.server.user.repository

import com.brifo.server.agent.entity.AgentType
import com.brifo.server.news.entity.NewsSource
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

interface UserHomeQueryRepository {
    fun getUserHomeData(
        userId: Long,
        todayStart: LocalDateTime,
        tomorrowStart: LocalDateTime,
    ): UserHomeData
}

data class UserHomeData(
    val agents: List<UserHomeAgent>,
    val todayDecisionCount: Long,
    val batchTime: LocalDateTime?,
    val newsCards: List<UserHomeNewsCard>,
)

data class UserHomeAgent(
    val agentId: UUID,
    val agentType: AgentType,
    val level: Int,
)

data class UserHomeNewsCard(
    val cardId: UUID,
    val headline: String,
    val newsId: UUID,
    val publishedAt: LocalDateTime,
    val source: NewsSource,
    val stockId: UUID,
    val stockName: String,
    val changeRate: BigDecimal?,
)
