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
    // FastAPI 서버 정책
    AI_CARD_NEWS(
        timeout = Duration.ofSeconds(15),
        maxRetries = 1,
    ),
    // 브리핑은 최대 3명(에이전트) 배치 요청까지 한 번에 처리되어 응답이 오래 걸린다.
    // 3명 요청 시 실측 약 40초 소요를 확인해 여유를 두고 60초로 설정한다.
    AI_BRIEFING(
        timeout = Duration.ofSeconds(60),
        maxRetries = 0,
    ),
}
