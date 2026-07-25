package com.brifo.server.user.repository

import com.brifo.server.news.entity.NewsSource
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

interface UserHomeQueryRepository {
    fun findTodayNewsCards(
        userId: Long,
        todayStart: LocalDateTime,
        tomorrowStart: LocalDateTime,
    ): List<UserHomeNewsCard>

    fun findLatestCompletedBatchTime(
        todayStart: LocalDateTime,
        tomorrowStart: LocalDateTime,
    ): LocalDateTime?
}

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
