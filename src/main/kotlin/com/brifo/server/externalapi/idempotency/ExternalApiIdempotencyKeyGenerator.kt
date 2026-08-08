package com.brifo.server.externalapi.idempotency

import java.time.LocalDate
import java.util.UUID

// 외부 API별 idempotency key 문자열만 생성한다.
// Redis 접근, DB 조회, 중복 판단은 하지 않는다.
object ExternalApiIdempotencyKeyGenerator {
    // 같은 종목의 1분 내 현재가 요청을 같은 작업으로 구분한다.
    fun stockPrice(
        stockCode: String,
        tradeDate: LocalDate,
    ) = "stock-price:$stockCode:$tradeDate"

    fun newsCollection(stockCode: String, targetDate: LocalDate) = "news-collection:$stockCode:$targetDate"

    fun disclosure(stockCode: String, date: LocalDate) = "disclosure:$stockCode:$date"

    // 뉴스 한 건의 카드뉴스 생성 작업을 구분한다.
    fun cardNews(newsId: UUID) = "card-news:$newsId"

    // 같은 카드뉴스와 사원의 기본 브리핑을 구분한다.
    // personalComment는 AI 서버가 처리하므로 userId는 포함하지 않는다.
    fun briefing(
        newsCardId: UUID,
        agentId: UUID,
    ): String {
        return "briefing:$newsCardId:$agentId"
    }

}
