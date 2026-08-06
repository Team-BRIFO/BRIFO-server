package com.brifo.server.news.client

import java.util.UUID

interface NewsCardGenerationClient {
    fun createNewsCard(request: Request): Result

    data class Request(
        val newsId: UUID,
        val stockName: String,
        val newsContent: String,
        val excludeTerms: List<String>,
    )

    data class Result(
        val newsId: UUID,
        val cardNews: List<CardNews>,
    )

    data class CardNews(
        val headline: String,
        val points: List<String>,
        val keywords: List<String>,
        val terms: List<GlossaryTerm> = emptyList(),
    )

    data class GlossaryTerm(
        val term: String,
        val definition: String,
        val surface: String? = null,
    )
}
