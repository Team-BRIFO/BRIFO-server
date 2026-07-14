package com.brifo.server.global.log.service

import com.brifo.server.TestcontainersConfiguration
import com.brifo.server.log.entity.ExternalApiCallStatus
import com.brifo.server.log.repository.ExternalApiCallLogRepository
import com.brifo.server.log.service.ExternalApiCallLogService
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
    fun `payloadOf는 모든 민감 필드를 재귀적으로 마스킹한다`() {
        val payload = externalApiCallLogService.payloadOf(
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
                        "2중" to mapOf(
                            "password" to "지워지나보자",
                        ),
                    ),
                ),
                "stockCode" to "005930",
            ),
        )

        println(
            """
            [payloadOf 테스트 결과]
            마스킹된 페이로드: $payload
            """.trimIndent() + "\n\n",
        )

        assertThat(payload!!["authorization"].asText()).isEqualTo("******")
        assertThat(payload["apiKey"].asText()).isEqualTo("******")
        assertThat(payload["user"]["accessToken"].asText()).isEqualTo("******")
        assertThat(payload["user"]["password"].asText()).isEqualTo("******")
        assertThat(payload["user"]["profile"]["appSecret"].asText()).isEqualTo("******")
        assertThat(payload["news"][0]["newsContent"].asText()).isEqualTo("******")
        assertThat(payload["news"][0]["2중"]["password"].asText()).isEqualTo("******")
        assertThat(payload["stockCode"].asText()).isEqualTo("005930")
        assertThat(payload["news"][0]["title"].asText()).isEqualTo("삼성전자 뉴스")
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

        println(
            """
            [saveSuccess 테스트 결과]
            ID: ${savedLog.id}
            API 이름: ${savedLog.apiName}
            제공자: ${savedLog.provider}
            상태: ${savedLog.status}
            HTTP 상태코드: ${savedLog.httpStatusCode}
            재시도 횟수: ${savedLog.retryCount}
            응답시간: ${savedLog.latencyMs}ms
            요청값: ${savedLog.requestPayload}
            응답값: ${savedLog.responsePayload}
            호출시간: ${savedLog.calledAt}
            """.trimIndent() + "\n\n",
        )

        assertThat(savedLog.status).isEqualTo(ExternalApiCallStatus.SUCCESS)
        assertThat(savedLog.apiName).isEqualTo("KIS_STOCK_PRICE")
        assertThat(savedLog.provider).isEqualTo("KIS")
        assertThat(savedLog.httpStatusCode).isEqualTo(200)
        // TODO: 실제 외부 API 재시도 로직 구현 후 retry count 증가를 통합 테스트로 검증한다.
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

        println(
            """
            [saveFail 테스트 결과]
            ID: ${savedLog.id}
            API 이름: ${savedLog.apiName}
            제공자: ${savedLog.provider}
            상태: ${savedLog.status}
            HTTP 상태코드: ${savedLog.httpStatusCode}
            재시도 횟수: ${savedLog.retryCount}
            응답시간: ${savedLog.latencyMs}ms
            요청값: ${savedLog.requestPayload}
            응답값: ${savedLog.responsePayload}
            호출시간: ${savedLog.calledAt}
            """.trimIndent() + "\n\n",
        )

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

        println(
            """
            [saveTimeout 테스트 결과]
            ID: ${savedLog.id}
            API 이름: ${savedLog.apiName}
            제공자: ${savedLog.provider}
            상태: ${savedLog.status}
            HTTP 상태코드: ${savedLog.httpStatusCode}
            재시도 횟수: ${savedLog.retryCount}
            응답시간: ${savedLog.latencyMs}ms
            요청값: ${savedLog.requestPayload}
            응답값: ${savedLog.responsePayload}
            호출시간: ${savedLog.calledAt}
            """.trimIndent() + "\n\n",
        )

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

        println(
            """
            [calculateLatencyMs 테스트 결과]
            시작시간: $startedAt
            측정된 응답시간: ${latencyMs}ms
            """.trimIndent() + "\n\n",
        )

        assertThat(latencyMs).isGreaterThanOrEqualTo(10)
    }
}
