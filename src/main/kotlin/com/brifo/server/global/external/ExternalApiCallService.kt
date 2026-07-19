package com.brifo.server.global.external

import com.brifo.server.log.dto.ExternalApiCallLogSaveData
import com.brifo.server.log.entity.ExternalApiCallStatus
import com.brifo.server.log.service.ExternalApiCallLogService
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.time.Instant
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

        while (true) {
            try {
                val response = request()
                val responseBody = checkNotNull(response.body) {
                    "$apiName API response body is empty"
                }

                saveSuccess(
                    provider = provider,
                    apiName = apiName,
                    requestPayload = requestPayload,
                    responsePayload = responseBody,
                    responseStatusCode = response.statusCode.value(),
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
                        saveFailure(
                            provider = provider,
                            apiName = apiName,
                            requestPayload = requestPayload,
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

                saveFailure(
                    provider = provider,
                    apiName = apiName,
                    requestPayload = requestPayload,
                    exception = exception,
                    responseStatusCode = exception.responseStatusCode(),
                    retryCount = totalRetryCount,
                    requestedAt = requestedAt,
                    startedAt = startedAt,
                )

                throw exception
            }
        }
    }

    // 최종 성공 결과를 저장한다.
    private fun saveSuccess(
        provider: String,
        apiName: String,
        requestPayload: Any?,
        responsePayload: Any?,
        responseStatusCode: Int,
        retryCount: Int,
        requestedAt: LocalDateTime,
        startedAt: Instant,
    ) {
        saveLog(
            provider = provider,
            apiName = apiName,
            status = ExternalApiCallStatus.SUCCESS,
            requestPayload = requestPayload,
            responsePayload = responsePayload,
            responseStatusCode = responseStatusCode,
            errorMessage = null,
            retryCount = retryCount,
            requestedAt = requestedAt,
            startedAt = startedAt,
        )
    }

    // 최종 실패 결과를 저장한다.
    private fun saveFailure(
        provider: String,
        apiName: String,
        requestPayload: Any?,
        exception: Throwable,
        responseStatusCode: Int?,
        retryCount: Int,
        requestedAt: LocalDateTime,
        startedAt: Instant,
    ) {
        saveLog(
            provider = provider,
            apiName = apiName,
            status = exception.toCallStatus(),
            requestPayload = requestPayload,
            responsePayload = null,
            responseStatusCode = responseStatusCode,
            errorMessage = exception.safeMessage(),
            retryCount = retryCount,
            requestedAt = requestedAt,
            startedAt = startedAt,
        )
    }

    // 기존 ExternalApiCallLogService를 이용해 최종 로그를 저장한다.
    private fun saveLog(
        provider: String,
        apiName: String,
        status: ExternalApiCallStatus,
        requestPayload: Any?,
        responsePayload: Any?,
        responseStatusCode: Int?,
        errorMessage: String?,
        retryCount: Int,
        requestedAt: LocalDateTime,
        startedAt: Instant,
    ) {
        logService.save(
            ExternalApiCallLogSaveData(
                provider = provider,
                apiName = apiName,
                status = status,

                // 기존 민감 정보 마스킹 기능
                requestPayloadRedacted = logService.redactPayload(requestPayload),
                responsePayloadRedacted = logService.redactPayload(responsePayload),
                responseStatusCode = responseStatusCode,
                errorMessage = errorMessage,
                retryCount = retryCount,

                // 기존 시간 계산 기능
                durationMs = logService.calculateDurationMs(startedAt),
                requestedAt = requestedAt,
                respondedAt = LocalDateTime.now(),
            ),
        )
    }
}
