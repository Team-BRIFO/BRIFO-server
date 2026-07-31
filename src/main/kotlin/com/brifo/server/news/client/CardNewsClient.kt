package com.brifo.server.news.client

// 카드뉴스 생성을 AI 서버에 요청하고 응답을 반환한다.
interface CardNewsClient {
    fun createCardNews(request: Request): Response

    data class Request(
        val newsId: String,
        val stockName: String,
        val newsContent: String,

        // 최근 14일 동안 이미 출제된 용어를 전달한다.
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
