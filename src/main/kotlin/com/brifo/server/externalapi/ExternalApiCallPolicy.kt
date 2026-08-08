package com.brifo.server.externalapi

import java.time.Duration

// 외부 API별 초기 timeout과 retry 정책
enum class ExternalApiCallPolicy(
    val timeout: Duration,
    val maxRetries: Int,
) {
    // timeout 3초, 최초 요청 실패 시 1회 추가 요청
    STOCK_PRICE(
        timeout = Duration.ofSeconds(5),
        maxRetries = 1,
    ),
    NEWS_COLLECTION(
        timeout = Duration.ofSeconds(5),
        maxRetries = 1,
    ),
    DISCLOSURE(
        timeout = Duration.ofSeconds(5),
        maxRetries = 1,
    ),
    // FastAPI 서버 정책
    AI_CARD_NEWS(
        timeout = Duration.ofSeconds(15),
        maxRetries = 1,
    ),
    // 브리핑은 응답 시간이 길어서 30초를 적용한다.
    AI_BRIEFING(
        timeout = Duration.ofSeconds(30),
        maxRetries = 0,
    ),
}
