package com.brifo.server.news.client

import com.brifo.server.news.entity.NewsSource
import java.time.LocalDate
import java.time.LocalDateTime

interface NewsCollectionClient {
    fun collect(request: Request): Result

    data class Request(
        val targetDate: LocalDate,
        val publishedUntil: LocalDateTime,
        val stocks: List<StockRef>,
    )

    data class StockRef(
        val code: String,
        val name: String,
    )

    data class Result(
        val news: List<CollectedNews>,
    )

    data class CollectedNews(
        val stockCode: String,
        val source: NewsSource,
        val sourceUrl: String,
        val sourceImageUrl: String? = null,
        val title: String,
        val summary: String?,
        val dedupKey: String,
        val publishedAt: LocalDateTime,
    )
}
