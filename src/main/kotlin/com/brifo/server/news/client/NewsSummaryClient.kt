package com.brifo.server.news.client

interface NewsSummaryClient {
    fun createCardNews(request: Request): Response

    data class Request(
        val newsId: String,
        val stockName: String,
        val newsContent: String,
        val excludeTerms: List<String>,
    )

    data class Response(
        val isSuccess: Boolean,
        val code: String,
        val message: String,
        val result: Result?,
    )

    data class Result(
        val newsId: String,
        val cardNews: List<CardNews>,
    )

    data class CardNews(
        val headline: String,
        val points: List<String>,
        val keywords: List<String>,
        val terms: List<Term>,
    )

    data class Term(
        val surface: String,
        val term: String,
        val definition: String,
    )
}
