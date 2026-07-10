package com.brifo.server.global.log.service

import com.brifo.server.TestcontainersConfiguration
import com.brifo.server.global.log.entity.ExternalApiCallStatus
import com.brifo.server.global.log.repository.ExternalApiCallLogRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@Import(TestcontainersConfiguration::class)
@ActiveProfiles("test")
@SpringBootTest
class ExternalApiCallLogServiceTest @Autowired constructor(
    private val externalApiCallLogService: ExternalApiCallLogService,
    private val externalApiCallLogRepository: ExternalApiCallLogRepository,
) {
    @BeforeEach
    fun setUp() {
        externalApiCallLogRepository.deleteAll()
    }

    @Test
    fun `payloadOf는 중첩된 민감 필드를 마스킹한다`() {
        val payload = externalApiCallLogService.payloadOf(
            mapOf(
                "headers" to mapOf(
                    "authorization" to "Bearer token",
                ),
                "body" to mapOf(
                    "stockCode" to "005930",
                    "user" to mapOf(
                        "password" to "1234",
                    ),
                    "items" to listOf(
                        mapOf("apiKey" to "secret-key"),
                    ),
                ),
            ),
        )

        assertThat(payload!!.get("headers").get("authorization").asText()).isEqualTo("******")
        assertThat(payload.get("body").get("user").get("password").asText()).isEqualTo("******")
        assertThat(payload.get("body").get("items").get(0).get("apiKey").asText()).isEqualTo("******")
        assertThat(payload.get("body").get("stockCode").asText()).isEqualTo("005930")
    }

    @Test
    fun `saveSuccess는 SUCCESS 로그와 주요 필드를 저장한다`() {
        val requestPayload = externalApiCallLogService.payloadOf(
            mapOf("stockCode" to "005930"),
        )
        val responsePayload = externalApiCallLogService.payloadOf(
            mapOf("price" to 72500),
        )

        externalApiCallLogService.saveSuccess(
            apiName = "KIS_STOCK_PRICE",
            provider = "KIS",
            requestPayload = requestPayload,
            responsePayload = responsePayload,
            httpStatusCode = 200,
            retryCount = 2,
            latencyMs = 123,
        )

        val savedLog = externalApiCallLogRepository.findAll().single()

        assertThat(savedLog.status).isEqualTo(ExternalApiCallStatus.SUCCESS)
        assertThat(savedLog.apiName).isEqualTo("KIS_STOCK_PRICE")
        assertThat(savedLog.provider).isEqualTo("KIS")
        assertThat(savedLog.httpStatusCode).isEqualTo(200)
        assertThat(savedLog.retryCount).isEqualTo(2)
        assertThat(savedLog.latencyMs).isEqualTo(123)
        assertThat(savedLog.requestPayload!!.get("stockCode").asText()).isEqualTo("005930")
        assertThat(savedLog.responsePayload!!.get("price").asInt()).isEqualTo(72500)
    }

    @Test
    fun `saveFail은 FAIL 로그를 저장한다`() {
        val requestPayload = externalApiCallLogService.payloadOf(
            mapOf("keyword" to "삼성전자"),
        )
        val responsePayload = externalApiCallLogService.payloadOf(
            mapOf("errorCode" to "API_ERROR"),
        )

        externalApiCallLogService.saveFail(
            apiName = "NEWS_SEARCH",
            provider = "NAVER",
            requestPayload = requestPayload,
            responsePayload = responsePayload,
            httpStatusCode = 500,
            retryCount = 1,
            latencyMs = 456,
        )

        val savedLog = externalApiCallLogRepository.findAll().single()

        assertThat(savedLog.status).isEqualTo(ExternalApiCallStatus.FAIL)
        assertThat(savedLog.apiName).isEqualTo("NEWS_SEARCH")
        assertThat(savedLog.provider).isEqualTo("NAVER")
        assertThat(savedLog.httpStatusCode).isEqualTo(500)
        assertThat(savedLog.retryCount).isEqualTo(1)
        assertThat(savedLog.latencyMs).isEqualTo(456)
        assertThat(savedLog.responsePayload!!.get("errorCode").asText()).isEqualTo("API_ERROR")
    }

    @Test
    fun `saveTimeout은 TIMEOUT 로그를 저장한다`() {
        val requestPayload = externalApiCallLogService.payloadOf(
            mapOf("newsId" to 1L),
        )

        externalApiCallLogService.saveTimeout(
            apiName = "FAST_API_BRIEFING",
            provider = "FAST_API",
            requestPayload = requestPayload,
            retryCount = 3,
            latencyMs = 3000,
        )

        val savedLog = externalApiCallLogRepository.findAll().single()

        assertThat(savedLog.status).isEqualTo(ExternalApiCallStatus.TIMEOUT)
        assertThat(savedLog.apiName).isEqualTo("FAST_API_BRIEFING")
        assertThat(savedLog.provider).isEqualTo("FAST_API")
        assertThat(savedLog.httpStatusCode).isNull()
        assertThat(savedLog.responsePayload).isNull()
        assertThat(savedLog.retryCount).isEqualTo(3)
        assertThat(savedLog.latencyMs).isEqualTo(3000)
    }

    @Test
    fun `calculateLatencyMs는 시작 시간 이후 경과 시간을 반환한다`() {
        val startedAt = externalApiCallLogService.startTimer()

        Thread.sleep(10)

        val latencyMs = externalApiCallLogService.calculateLatencyMs(startedAt)

        assertThat(latencyMs).isGreaterThanOrEqualTo(10)
    }
}
