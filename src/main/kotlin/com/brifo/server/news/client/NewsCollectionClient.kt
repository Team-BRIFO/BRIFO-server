package com.brifo.server.news.client

import com.brifo.server.news.entity.NewsSource
import java.time.LocalDate
import java.time.LocalDateTime

interface NewsCollectionClient {
    fun collect(request: Request): Result

    data class Request(
        val targetDate: LocalDate,
        val publishedUntil: LocalDateTime,
    )

    data class Result(
        val news: List<CollectedNews>,
    )

    data class CollectedNews(
        val stockCode: String,
        val source: NewsSource,
        val sourceUrl: String,
        val title: String,
        val summary: String?,
        val dedupKey: String,
        val publishedAt: LocalDateTime,
        val importantKeywords: Set<String> = emptySet(),
    )
}
