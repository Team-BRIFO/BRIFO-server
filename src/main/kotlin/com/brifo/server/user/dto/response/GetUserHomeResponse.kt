package com.brifo.server.user.dto.response

import com.brifo.server.agent.entity.AgentType
import com.brifo.server.news.entity.NewsSource
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class GetUserHomeResponse(
    val user: User,
    val agents: List<HomeAgent>,
    val attendedToday: Boolean,
    val weeklyAttendanceDays: Int,
    val dates: List<LocalDate>,
    val todayDecisions: TodayDecisions,
    val todayNewsCards: TodayNewsCards,
) {
    data class User(
        val nickname: String,
        val companyName: String,
        val balanceAp: Int,
    )

    data class HomeAgent(
        val agentId: UUID,
        val agentType: AgentType,
        val level: Int,
    )

    data class TodayDecisions(
        val count: Int,
    )

    data class TodayNewsCards(
        val batchTime: LocalDateTime?,
        val items: List<NewsCardItem>,
    ) {
        data class NewsCardItem(
            val cardId: UUID,
            val headline: String,
            val news: News,
            val stock: HomeNewsStock,
        )

        data class News(
            val newsId: UUID,
            val publishedAt: LocalDateTime,
            val source: NewsSource,
        )

        data class HomeNewsStock(
            val stockId: UUID,
            val name: String,
            val changeRate: BigDecimal?,
        )
    }
}
