package com.brifo.server.externalapi.idempotency

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

// 외부 API별 idempotency key 문자열만 생성한다.
// Redis 접근, DB 조회, 중복 판단은 하지 않는다.
object ExternalApiIdempotencyKeyGenerator {
    // 같은 종목의 1분 내 현재가 요청을 같은 작업으로 구분한다.
    fun kisCurrentPrice(stockCode: String): String {
        val minuteBucket = LocalDateTime
            .now(ZoneId.of("Asia/Seoul"))
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))

        return "kis-current-price:$stockCode:$minuteBucket"
    }

    // 같은 종목과 거래일의 종가 요청을 같은 작업으로 구분한다.
    fun kisDailyPrice(
        stockCode: String,
        tradeDate: LocalDate,
    ): String {
        return "kis-daily-price:$stockCode:$tradeDate"
    }

    // 정규화된 URL을 해시해 네이버 뉴스 key를 생성한다.
    fun naverNews(normalizedUrl: String): String {
        return "naver-news:${sha256(normalizedUrl)}"
    }

    // DART 접수번호를 이용해 공시 key를 생성한다.
    fun dartDisclosure(rceptNo: String): String {
        return "dart:$rceptNo"
    }

    // 정규화된 URL을 해시해 KRX 공시 key를 생성한다.
    fun krxDisclosure(normalizedUrl: String): String {
        return "krx:${sha256(normalizedUrl)}"
    }

    // 뉴스 한 건의 카드뉴스 생성 작업을 구분한다.
    fun cardSummary(newsId: UUID): String {
        return "card-summary:news:$newsId"
    }

    // 같은 카드뉴스와 사원의 기본 브리핑을 구분한다.
    // personalComment는 AI 서버가 처리하므로 userId는 포함하지 않는다.
    fun briefing(
        newsCardId: UUID,
        agentId: UUID,
    ): String {
        return "briefing:$newsCardId:$agentId"
    }

    // URL을 일정한 길이의 문자열로 변환한다.
    private fun sha256(value: String): String {
        return MessageDigest
            .getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { byte ->
                "%02x".format(byte)
            }
    }
}
