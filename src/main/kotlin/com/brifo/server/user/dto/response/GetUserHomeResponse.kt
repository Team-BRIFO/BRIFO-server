package com.brifo.server.user.dto.response

import com.brifo.server.agent.entity.AgentType
import com.brifo.server.news.entity.NewsSource
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class GetUserHomeResponse(
    val user: User,
    val agents: List<Agent>,
    val todayDecisions: TodayDecisions,
    val todayNewsCards: TodayNewsCards,
) {
    data class User(
        val nickname: String,
        val companyName: String,
        val balanceAp: Int,
    )

    data class Agent(
        val agentId: UUID,
        val agentType: AgentType,
        val level: Int,
    )

    data class TodayDecisions(
        val count: Int,
    )

    data class TodayNewsCards(
        val batchTime: LocalDateTime?,
        val items: List<Item>,
    ) {
        data class Item(
            val cardId: UUID,
            val headline: String,
            val news: News,
            val stock: Stock,
        )

        data class News(
            val newsId: UUID,
            val publishedAt: LocalDateTime,
            val source: NewsSource,
        )

        data class Stock(
            val stockId: UUID,
            val name: String,
            val changeRate: BigDecimal?,
        )
    }
}
