package com.brifo.server.log.service

import com.brifo.server.log.entity.ExternalApiCallLog
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class ExternalApiCallLogServiceUnitTest {
    private val writer = object : ExternalApiCallLogWriter {
        override fun save(externalApiCallLog: ExternalApiCallLog) = Unit
    }
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
}
