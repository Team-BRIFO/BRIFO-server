package com.brifo.server.log.service

import com.brifo.server.log.entity.ExternalApiCallLog
import com.brifo.server.log.entity.ExternalApiCallStatus
import com.brifo.server.log.repository.ExternalApiCallLogRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import java.time.LocalDateTime

class ExternalApiCallLogServiceFailureTest {
    // 실제 DB를 사용하지 않고 가짜 repository 생성
    private val repository = mock(ExternalApiCallLogRepository::class.java)

    // 테스트할 service에 가짜 repository 주입
    private val service = ExternalApiCallLogService(repository, ObjectMapper())

    @Test
    fun `로그 저장 실패는 비즈니스 로직으로 예외를 전파하지 않는다`() {
        // repository.save() 호출 시 DB 저장 실패 예외가 발생하도록 설정
        doThrow(RuntimeException("DB 저장 실패"))
            .`when`(repository)
            .save(any(ExternalApiCallLog::class.java))

        // service 호출 중 발생한 예외가 외부로 나오는지 확인
        // 성공하면 Result.success(반환값), 실패하면 Result.failure(예외)
        val result =
            runCatching {
                service.save(
                    ExternalApiCallLogCommand(
                        provider = "KIS",
                        apiName = "KIS_STOCK_PRICE",
                        status = ExternalApiCallStatus.SUCCESS,
                        requestPayloadRedacted = service.redactPayload(mapOf("stockCode" to "005930")),
                        responsePayloadRedacted = service.redactPayload(mapOf("price" to 72500)),
                        responseStatusCode = 200,
                        retryCount = 0,
                        durationMs = 123L,
                        requestedAt = LocalDateTime.of(2026, 7, 15, 10, 0),
                        respondedAt = LocalDateTime.of(2026, 7, 15, 10, 0, 0, 123_000_000),
                    ),
                )
            }

        assertThat(result.isSuccess).isTrue()
    }
}
