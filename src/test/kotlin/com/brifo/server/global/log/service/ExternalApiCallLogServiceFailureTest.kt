package com.brifo.server.global.log.service

import com.brifo.server.global.log.entity.ExternalApiCallLog
import com.brifo.server.global.log.repository.ExternalApiCallLogRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock

class ExternalApiCallLogServiceFailureTest {
    // 실제 DB를 사용하지 않고 가짜 repository 생성
    private val repository = mock(ExternalApiCallLogRepository::class.java)

    // 테스트할 service에 가짜 repository 주입
    private val service = ExternalApiCallLogService(repository)

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
                service.saveSuccess(
                    apiName = "KIS_STOCK_PRICE",
                    provider = "KIS",
                    requestPayload =
                        service.payloadOf(
                            mapOf("stockCode" to "005930"),
                        ),
                    responsePayload =
                        service.payloadOf(
                            mapOf("price" to 72500),
                        ),
                    httpStatusCode = 200,
                    retryCount = 0,
                    latencyMs = 123,
                )
            }

        // service 밖으로 나온 예외를 가져옴
        // 예외가 나오지 않았다면 null
        val capturedException = result.exceptionOrNull()

        // 테스트 결과를 직접 확인하기 위한 출력
        println(
            """
            [로그 저장 실패 테스트 결과]
            Repository 발생 예외: DB 저장 실패
            비즈니스 로직으로 예외 전파: ${capturedException?.message ?: "없음"}
            테스트 결과: ${if (result.isSuccess) "PASS" else "FAIL"}
            """.trimIndent() + "\n\n",
        )

        // repository.save()의 예외를 service 내부에서 처리했다면 null
        assertThat(capturedException).isNull()
    }
}
