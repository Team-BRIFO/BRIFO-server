package com.brifo.server.payment.dto.response

data class ReadyPaymentResponse(
    val orderId: String,
    val amount: Int,
    /** 프론트가 토스페이먼츠 SDK를 초기화할 때 쓰는 공개 키. 시크릿 키와 달리 노출돼도 안전하다. */
    val clientKey: String,
)
