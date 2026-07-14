package com.brifo.server.global.log.service

import com.brifo.server.TestcontainersConfiguration
import com.brifo.server.log.entity.ExternalApiCallStatus
import com.brifo.server.log.repository.ExternalApiCallLogRepository
import com.brifo.server.log.service.ExternalApiCallLogCommand
import com.brifo.server.log.service.ExternalApiCallLogService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.LocalDateTime

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
    fun `redactPayload는 모든 민감 필드를 재귀적으로 마스킹한다`() {
        val payload = externalApiCallLogService.redactPayload(
            mapOf(
                "authorization" to "Bearer token",
                "apiKey" to "api-key",
                "user" to mapOf(
                    "accessToken" to "access-token",
                    "password" to "password-1234",
                    "profile" to mapOf(
                        "appSecret" to "app-secret",
                    ),
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
    fun `SUCCESS 로그는 ERD의 주요 필드를 저장한다`() {
        val requestedAt = LocalDateTime.of(2026, 7, 15, 10, 0)
        val respondedAt = requestedAt.plusNanos(123_000_000)
        val requestPayload = externalApiCallLogService.redactPayload(mapOf("stockCode" to "005930"))
        val responsePayload = externalApiCallLogService.redactPayload(mapOf("price" to 72500))

        externalApiCallLogService.save(
            ExternalApiCallLogCommand(
                provider = "KIS",
                apiName = "KIS_STOCK_PRICE",
                status = ExternalApiCallStatus.SUCCESS,
                userId = 1L,
                stockId = 2L,
                idempotencyKey = "stock-price-005930-20260715",
                requestPayloadRedacted = requestPayload,
                responsePayloadRedacted = responsePayload,
                responseStatusCode = 200,
                retryCount = 2,
                durationMs = 123L,
                totalTokens = 100,
                estimatedCostKrw = BigDecimal("12.3456"),
                requestedAt = requestedAt,
                respondedAt = respondedAt,
            ),
        )

        val savedLog = externalApiCallLogRepository.findAll().single()

        assertThat(savedLog.provider).isEqualTo("KIS")
        assertThat(savedLog.apiName).isEqualTo("KIS_STOCK_PRICE")
        assertThat(savedLog.status).isEqualTo(ExternalApiCallStatus.SUCCESS)
        assertThat(savedLog.userId).isEqualTo(1L)
        assertThat(savedLog.stockId).isEqualTo(2L)
        assertThat(savedLog.idempotencyKey).isEqualTo("stock-price-005930-20260715")
        assertThat(savedLog.requestPayloadRedacted!!["stockCode"].asText()).isEqualTo("005930")
        assertThat(savedLog.responsePayloadRedacted!!["price"].asInt()).isEqualTo(72500)
        assertThat(savedLog.responseStatusCode).isEqualTo(200)
        assertThat(savedLog.retryCount).isEqualTo(2)
        assertThat(savedLog.durationMs).isEqualTo(123L)
        assertThat(savedLog.totalTokens).isEqualTo(100)
        assertThat(savedLog.estimatedCostKrw).isEqualByComparingTo("12.3456")
        assertThat(savedLog.requestedAt).isEqualTo(requestedAt)
        assertThat(savedLog.respondedAt).isEqualTo(respondedAt)
        assertThat(savedLog.createdAt).isNotNull()
    }

    @Test
    fun `FAIL 로그는 실패 정보와 관련 ID를 저장한다`() {
        val requestedAt = LocalDateTime.of(2026, 7, 15, 11, 0)
        val respondedAt = requestedAt.plusNanos(456_000_000)

        externalApiCallLogService.save(
            ExternalApiCallLogCommand(
                provider = "NAVER",
                apiName = "NEWS_SEARCH",
                status = ExternalApiCallStatus.FAIL,
                newsId = 3L,
                requestPayloadRedacted = externalApiCallLogService.redactPayload(mapOf("keyword" to "삼성전자")),
                responsePayloadRedacted = externalApiCallLogService.redactPayload(mapOf("errorCode" to "API_ERROR")),
                responseStatusCode = 500,
                errorMessage = "외부 API 오류",
                retryCount = 1,
                durationMs = 456L,
                requestedAt = requestedAt,
                respondedAt = respondedAt,
            ),
        )

        val savedLog = externalApiCallLogRepository.findAll().single()

        assertThat(savedLog.status).isEqualTo(ExternalApiCallStatus.FAIL)
        assertThat(savedLog.newsId).isEqualTo(3L)
        assertThat(savedLog.responseStatusCode).isEqualTo(500)
        assertThat(savedLog.errorMessage).isEqualTo("외부 API 오류")
        assertThat(savedLog.retryCount).isEqualTo(1)
        assertThat(savedLog.durationMs).isEqualTo(456L)
    }

    @Test
    fun `TIMEOUT 로그는 응답 없이 타임아웃 정보를 저장한다`() {
        val requestedAt = LocalDateTime.of(2026, 7, 15, 12, 0)
        val respondedAt = requestedAt.plusSeconds(3)

        externalApiCallLogService.save(
            ExternalApiCallLogCommand(
                provider = "FAST_API",
                apiName = "FAST_API_BRIEFING",
                status = ExternalApiCallStatus.TIMEOUT,
                briefingId = 4L,
                requestPayloadRedacted = externalApiCallLogService.redactPayload(mapOf("newsId" to 1L)),
                errorMessage = "응답 시간 초과",
                retryCount = 3,
                durationMs = 3000L,
                requestedAt = requestedAt,
                respondedAt = respondedAt,
            ),
        )

        val savedLog = externalApiCallLogRepository.findAll().single()

        assertThat(savedLog.status).isEqualTo(ExternalApiCallStatus.TIMEOUT)
        assertThat(savedLog.briefingId).isEqualTo(4L)
        assertThat(savedLog.responsePayloadRedacted).isNull()
        assertThat(savedLog.responseStatusCode).isNull()
        assertThat(savedLog.errorMessage).isEqualTo("응답 시간 초과")
        assertThat(savedLog.retryCount).isEqualTo(3)
        assertThat(savedLog.durationMs).isEqualTo(3000L)
    }

    @Test
    fun `calculateDurationMs는 시작 시간 이후 경과 시간을 반환한다`() {
        val startedAt = externalApiCallLogService.startTimer()

        Thread.sleep(10)

        val durationMs = externalApiCallLogService.calculateDurationMs(startedAt)

        assertThat(durationMs).isGreaterThanOrEqualTo(10L)
    }
}
