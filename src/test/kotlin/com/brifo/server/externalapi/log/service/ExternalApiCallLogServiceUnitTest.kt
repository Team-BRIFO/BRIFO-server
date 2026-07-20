package com.brifo.server.externalapi.log.service

import com.brifo.server.externalapi.log.entity.ExternalApiCallLog
import com.brifo.server.externalapi.log.entity.ExternalApiCallStatus
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.web.client.ResourceAccessException
import java.net.SocketTimeoutException
import java.time.Instant
import java.time.LocalDateTime

class ExternalApiCallLogServiceUnitTest {
    private val writer = RecordingLogWriter()
    private val service = ExternalApiCallLogService(writer, ObjectMapper())

    @Test
    fun `redactPayload는 모든 민감 필드를 재귀적으로 마스킹한다`() {
        val payload = service.redactPayload(
            mapOf(
                "authorization" to "Bearer token",
                "apiKey" to "api-key",
                "user" to mapOf(
                    "accessToken" to "access-token",
                    "password" to "password-1234",
                    "profile" to mapOf("appSecret" to "app-secret"),
                ),
                "news" to listOf(
                    mapOf(
                        "title" to "삼성전자 뉴스",
                        "newsContent" to "민감한 뉴스 원문",
                    ),
                ),
                "stockCode" to "005930",
            ),
        )

        assertThat(payload!!["authorization"].asText()).isEqualTo("******")
        assertThat(payload["apiKey"].asText()).isEqualTo("******")
        assertThat(payload["user"]["accessToken"].asText()).isEqualTo("******")
        assertThat(payload["user"]["password"].asText()).isEqualTo("******")
        assertThat(payload["user"]["profile"]["appSecret"].asText()).isEqualTo("******")
        assertThat(payload["news"][0]["newsContent"].asText()).isEqualTo("******")
        assertThat(payload["stockCode"].asText()).isEqualTo("005930")
        assertThat(payload["news"][0]["title"].asText()).isEqualTo("삼성전자 뉴스")
    }

    @Test
    fun `calculateDurationMs는 두 시각 사이의 경과 시간을 반환한다`() {
        val startedAt = Instant.parse("2026-07-15T00:00:00Z")
        val endedAt = startedAt.plusMillis(123)

        val durationMs = service.calculateDurationMs(startedAt, endedAt)

        assertThat(durationMs).isEqualTo(123L)
    }

    @Test
    fun `saveLog는 예외가 없으면 SUCCESS 로그를 저장한다`() {
        val requestedAt = LocalDateTime.of(2026, 7, 20, 10, 0)

        service.saveLog(
            provider = "KIS",
            apiName = "KIS_STOCK_PRICE",
            requestPayload = mapOf(
                "authorization" to "Bearer token",
                "stockCode" to "005930",
            ),
            responsePayload = mapOf(
                "price" to 72_500,
            ),
            responseStatusCode = 200,
            exception = null,
            retryCount = 1,
            requestedAt = requestedAt,
            startedAt = service.startTimer(),
        )

        val savedLog = checkNotNull(writer.savedLog)

        assertThat(savedLog.status)
            .isEqualTo(ExternalApiCallStatus.SUCCESS)
        assertThat(savedLog.requestPayloadRedacted!!["authorization"].asText())
            .isEqualTo("******")
        assertThat(savedLog.requestPayloadRedacted!!["stockCode"].asText())
            .isEqualTo("005930")
        assertThat(savedLog.responsePayloadRedacted!!["price"].asInt())
            .isEqualTo(72_500)
        assertThat(savedLog.responseStatusCode)
            .isEqualTo(200)
        assertThat(savedLog.errorMessage)
            .isNull()
        assertThat(savedLog.retryCount)
            .isEqualTo(1)
    }

    @Test
    fun `saveLog는 일반 예외를 FAIL로 저장한다`() {
        service.saveLog(
            provider = "KIS",
            apiName = "KIS_STOCK_PRICE",
            requestPayload = mapOf(
                "stockCode" to "005930",
            ),
            responsePayload = mapOf(
                "ignored" to true,
            ),
            responseStatusCode = 500,
            exception = IllegalStateException("external API failed"),
            retryCount = 1,
            requestedAt = LocalDateTime.of(2026, 7, 20, 10, 0),
            startedAt = service.startTimer(),
        )

        val savedLog = checkNotNull(writer.savedLog)

        assertThat(savedLog.status)
            .isEqualTo(ExternalApiCallStatus.FAIL)
        assertThat(savedLog.responsePayloadRedacted)
            .isNull()
        assertThat(savedLog.responseStatusCode)
            .isEqualTo(500)
        assertThat(savedLog.errorMessage)
            .isEqualTo("external API failed")
        assertThat(savedLog.retryCount)
            .isEqualTo(1)
    }

    @Test
    fun `saveLog는 원인 예외가 timeout이면 TIMEOUT으로 저장한다`() {
        val exception = ResourceAccessException(
            "read timeout",
            SocketTimeoutException("read timeout"),
        )

        service.saveLog(
            provider = "FAST_API",
            apiName = "FAST_API_BRIEFING",
            requestPayload = mapOf(
                "newsId" to 1L,
            ),
            responsePayload = null,
            responseStatusCode = 504,
            exception = exception,
            retryCount = 1,
            requestedAt = LocalDateTime.of(2026, 7, 20, 10, 0),
            startedAt = service.startTimer(),
        )

        val savedLog = checkNotNull(writer.savedLog)

        assertThat(savedLog.status)
            .isEqualTo(ExternalApiCallStatus.TIMEOUT)
        assertThat(savedLog.responsePayloadRedacted)
            .isNull()
        assertThat(savedLog.responseStatusCode)
            .isNull()
        assertThat(savedLog.errorMessage)
            .isEqualTo("read timeout")
        assertThat(savedLog.retryCount)
            .isEqualTo(1)
    }

    @Test
    fun `saveLog는 오류 메시지를 2000자로 제한한다`() {
        val longMessage = "a".repeat(2_001)

        service.saveLog(
            provider = "TEST_PROVIDER",
            apiName = "TEST_API",
            requestPayload = null,
            responsePayload = null,
            responseStatusCode = null,
            exception = IllegalStateException(longMessage),
            retryCount = 0,
            requestedAt = LocalDateTime.of(2026, 7, 20, 10, 0),
            startedAt = service.startTimer(),
        )

        val savedLog = checkNotNull(writer.savedLog)

        assertThat(savedLog.status)
            .isEqualTo(ExternalApiCallStatus.FAIL)
        assertThat(savedLog.errorMessage)
            .hasSize(2_000)
            .isEqualTo("a".repeat(2_000))
    }

    private class RecordingLogWriter : ExternalApiCallLogWriter {
        var savedLog: ExternalApiCallLog? = null
            private set

        override fun save(
            externalApiCallLog: ExternalApiCallLog,
        ) {
            savedLog = externalApiCallLog
        }
    }
}
