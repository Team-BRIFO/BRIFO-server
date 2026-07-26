package com.brifo.server.externalapi

// 외부 API 호출 로그에 저장할 추적 정보를 하나로 묶는다.
// 이 객체는 중복 여부를 직접 판단하지 않는다.
data class ExternalApiCallContext(
    // 동일한 외부 API 작업을 식별하는 key
    val idempotencyKey: String? = null,

    // 호출과 관련된 내부 DB ID
    val userId: Long? = null,
    val stockId: Long? = null,
    val newsId: Long? = null,
    val briefingId: Long? = null,
)
