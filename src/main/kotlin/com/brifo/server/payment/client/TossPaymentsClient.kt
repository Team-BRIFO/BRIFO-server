package com.brifo.server.payment.client

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import tools.jackson.databind.ObjectMapper

@Component
class TossPaymentsClient(
    @Qualifier("tossPaymentsRestClient") private val restClient: RestClient,
    private val objectMapper: ObjectMapper,
) {
    data class ConfirmedPayment(
        val paymentKey: String,
        val orderId: String,
        val totalAmount: Int,
        val method: String?,
        val approvedAt: String?,
    )

    private data class TossConfirmResponse(
        val paymentKey: String,
        val orderId: String,
        val totalAmount: Int,
        val method: String?,
        val approvedAt: String?,
    )

    private data class TossErrorResponse(
        val code: String?,
        val message: String?,
    )

    /**
     * 토스페이먼츠 결제 승인 API를 호출한다.
     *
     * 카드 승인 거절 같은 정상적인 실패도 4xx로 내려오므로 예외를 던지는 대신 Result로
     * 감싼다 — 호출부(PaymentService)가 "우리 서버 버그"와 "고객 카드 문제"를 구분해서
     * 처리해야 하기 때문이다.
     */
    fun confirm(
        paymentKey: String,
        orderId: String,
        amount: Int,
    ): Result<ConfirmedPayment> =
        try {
            val response =
                restClient
                    .post()
                    .uri("/v1/payments/confirm")
                    .body(mapOf("paymentKey" to paymentKey, "orderId" to orderId, "amount" to amount))
                    .retrieve()
                    .body(TossConfirmResponse::class.java)
            val body = checkNotNull(response) { "토스페이먼츠 응답 본문이 없습니다." }
            Result.success(
                ConfirmedPayment(
                    paymentKey = body.paymentKey,
                    orderId = body.orderId,
                    totalAmount = body.totalAmount,
                    method = body.method,
                    approvedAt = body.approvedAt,
                ),
            )
        } catch (exception: RestClientResponseException) {
            val message = parseErrorMessage(exception) ?: "결제 승인에 실패했습니다."
            log.warn("토스페이먼츠 결제 승인이 거절됐습니다. orderId={}, message={}", orderId, message)
            Result.failure(TossPaymentDeclinedException(message))
        }

    private fun parseErrorMessage(exception: RestClientResponseException): String? =
        runCatching {
            objectMapper.readValue(exception.responseBodyAsString, TossErrorResponse::class.java).message
        }.getOrNull()

    class TossPaymentDeclinedException(message: String) : RuntimeException(message)

    private companion object {
        val log = LoggerFactory.getLogger(TossPaymentsClient::class.java)
    }
}
