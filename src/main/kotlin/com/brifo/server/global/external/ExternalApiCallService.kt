package com.brifo.server.global.external

import com.brifo.server.log.service.ExternalApiCallLogService
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ExternalApiCallService(
    private val logService: ExternalApiCallLogService,
) {
    fun <T : Any> execute(
        provider: String,
        apiName: String,
        policy: ExternalApiCallPolicy,
        // 조회처럼 중복 실행 문제가 없는 요청만 true로 지정한다.
        idempotent: Boolean = false,
        requestPayload: Any? = null,
        // 401에서 토큰 갱신이 가능한 API만 전달한다.
        refreshToken: (() -> Unit)? = null,
        // 실제 외부 API 요청
        request: () -> ResponseEntity<T>,
    ): T {
        val startedAt = logService.startTimer()
        val requestedAt = LocalDateTime.now()
        var totalRetryCount = 0
        var networkRetryCount = 0
        var tokenRefreshed = false

        // 네트워크 재시도와 토큰 갱신 후 재요청을 합친 최대 횟수
        val maxTotalRetries =
            policy.maxRetries +
                if (idempotent && refreshToken != null) 1 else 0

        // 최초 요청은 retryCount가 0이므로 최대 재시도 횟수까지 실행한다.
        while (totalRetryCount <= maxTotalRetries) {
            try {
                val response = request()
                val responseBody = checkNotNull(response.body) {
                    "$apiName API response body is empty"
                }

                logService.saveLog(
                    provider = provider,
                    apiName = apiName,
                    requestPayload = requestPayload,
                    responsePayload = responseBody,
                    responseStatusCode = response.statusCode.value(),
                    exception = null,
                    retryCount = totalRetryCount,
                    requestedAt = requestedAt,
                    startedAt = startedAt,
                )

                return responseBody
            } catch (exception: Exception) {
                // 멱등 요청만 토큰 갱신 후 원 요청을 한 번 다시 실행한다.
                if (
                    idempotent &&
                    exception.isUnauthorized() &&
                    refreshToken != null &&
                    !tokenRefreshed
                ) {
                    try {
                        refreshToken()
                    } catch (refreshException: Exception) {
                        logService.saveLog(
                            provider = provider,
                            apiName = apiName,
                            requestPayload = requestPayload,
                            responsePayload = null,
                            exception = refreshException,
                            responseStatusCode = 401,
                            retryCount = totalRetryCount,
                            requestedAt = requestedAt,
                            startedAt = startedAt,
                        )

                        throw refreshException
                    }

                    tokenRefreshed = true
                    totalRetryCount++
                    continue
                }

                // 멱등 요청의 네트워크·5xx 오류만 정책 범위에서 재시도한다.
                if (
                    idempotent &&
                    exception.isRetryable() &&
                    networkRetryCount < policy.maxRetries
                ) {
                    networkRetryCount++
                    totalRetryCount++
                    continue
                }

                logService.saveLog(
                    provider = provider,
                    apiName = apiName,
                    requestPayload = requestPayload,
                    responsePayload = null,
                    exception = exception,
                    responseStatusCode = exception.responseStatusCode(),
                    retryCount = totalRetryCount,
                    requestedAt = requestedAt,
                    startedAt = startedAt,
                )

                throw exception
            }
        }

        error("$apiName API request exceeded the maximum retry count")
    }

}
