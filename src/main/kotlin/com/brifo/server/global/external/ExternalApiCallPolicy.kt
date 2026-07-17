package com.brifo.server.global.external

import java.time.Duration

// 외부 API별 초기 timeout과 retry 정책
enum class ExternalApiCallPolicy(
    val timeout: Duration,
    val maxRetries: Int,
) {
    // timeout 3초, 최초 요청 실패 시 1회 추가 요청
    KIS_CURRENT_PRICE(
        timeout = Duration.ofSeconds(3),
        maxRetries = 1,
    ),
    // timeout 5초, 최초 요청 실패 시 1회 추가 요청
    KIS_CLOSING_PRICE(
        timeout = Duration.ofSeconds(5),
        maxRetries = 1,
    ),
    // 뉴스와 공시 API 공통 정책
    NEWS_DISCLOSURE(
        timeout = Duration.ofSeconds(5),
        maxRetries = 1,
    ),
    // FastAPI 서버 정책
    FAST_API(
        timeout = Duration.ofSeconds(15),
        maxRetries = 1,
    ),
}
