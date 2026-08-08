package com.brifo.server.externalapi

import com.brifo.server.externalapi.log.entity.ExternalApiCallLog
import com.brifo.server.externalapi.log.entity.ExternalApiCallStatus
import com.brifo.server.externalapi.log.service.ExternalApiCallLogService
import com.brifo.server.externalapi.log.service.ExternalApiCallLogWriter
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.nio.charset.StandardCharsets

class ExternalApiCallServiceUnitTest {
    private val writer = RecordingLogWriter()

    private val service =
        ExternalApiCallService(
            ExternalApiCallLogService(
                externalApiCallLogWriter = writer,
                objectMapper = ObjectMapper(),
            ),
        )

    @ParameterizedTest
    @ValueSource(ints = [500, 502, 503, 504])
    fun `retry 대상 5xx는 한 번 재시도한다`(
        statusCode: Int,
    ) {
        var attempts = 0

        val result = execute {
            attempts++

            // 첫 번째 요청은 실패하고 두 번째 요청은 성공한다.
            if (attempts == 1) {
                throw serverException(statusCode)
            }

            ResponseEntity.ok("success")
        }

        assertThat(result).isEqualTo("success")
        assertThat(attempts).isEqualTo(2)

        assertLog(
            status = ExternalApiCallStatus.SUCCESS,
            statusCode = 200,
            retryCount = 1,
        )
    }

    @ParameterizedTest
    @ValueSource(ints = [500, 502, 503, 504])
    fun `retry 대상 5xx가 반복되면 한 번 재시도 후 실패한다`(
        statusCode: Int,
    ) {
        var attempts = 0

        assertThatThrownBy {
            execute {
                attempts++
                throw serverException(statusCode)
            }
        }.isInstanceOf(HttpServerErrorException::class.java)

        assertThat(attempts).isEqualTo(2)

        assertLog(
            status = ExternalApiCallStatus.FAIL,
            statusCode = statusCode,
            retryCount = 1,
        )
    }

    @Test
    fun `501은 retry 대상인 500 502 503 504에 해당하지 않아 최종 FAIL로 처리된다`() {

        var attempts = 0

        assertThatThrownBy {
            execute {
                attempts++

                throw serverException(
                    HttpStatus.NOT_IMPLEMENTED.value(),
                )
            }
        }.isInstanceOf(HttpServerErrorException::class.java)

        assertThat(attempts).isEqualTo(1)

        assertLog(
            status = ExternalApiCallStatus.FAIL,
            statusCode = 501,
            retryCount = 0,
        )
    }

    @Test
    fun `timeout은 한 번 재시도하고 TIMEOUT으로 기록한다`() {
        var attempts = 0

        assertThatThrownBy {
            execute {
                attempts++

                throw ResourceAccessException(
                    "read timeout",
                    SocketTimeoutException("read timeout"),
                )
            }
        }.isInstanceOf(ResourceAccessException::class.java)

        assertThat(attempts).isEqualTo(2)

        assertLog(
            status = ExternalApiCallStatus.TIMEOUT,
            statusCode = null,
            retryCount = 1,
        )
    }

    @Test
    fun `일시적인 연결 오류는 한 번 재시도한다`() {
        var attempts = 0

        assertThatThrownBy {
            execute {
                attempts++

                throw ResourceAccessException(
                    "connection failed",
                    ConnectException("connection refused"),
                )
            }
        }.isInstanceOf(ResourceAccessException::class.java)

        assertThat(attempts).isEqualTo(2)

        assertLog(
            status = ExternalApiCallStatus.FAIL,
            statusCode = null,
            retryCount = 1,
        )
    }

    @ParameterizedTest
    @ValueSource(ints = [400, 403, 404])
    fun `일반적인 4xx는 재시도하지 않는다`(
        statusCode: Int,
    ) {
        var attempts = 0

        assertThatThrownBy {
            execute {
                attempts++
                throw clientException(statusCode)
            }
        }.isInstanceOf(HttpClientErrorException::class.java)

        assertThat(attempts).isEqualTo(1)

        assertLog(
            status = ExternalApiCallStatus.FAIL,
            statusCode = statusCode,
            retryCount = 0,
        )
    }

    @Test
    fun `429는 재시도하지 않고 실패로 기록한다`() {
        var attempts = 0

        assertThatThrownBy {
            execute {
                attempts++

                throw clientException(
                    HttpStatus.TOO_MANY_REQUESTS.value(),
                )
            }
        }.isInstanceOf(HttpClientErrorException::class.java)

        assertThat(attempts).isEqualTo(1)

        assertLog(
            status = ExternalApiCallStatus.FAIL,
            statusCode = 429,
            retryCount = 0,
        )
    }

    @Test
    fun `401은 토큰 갱신 후 원 요청을 한 번 다시 실행한다`() {
        var attempts = 0
        var refreshCount = 0

        val result = execute(
            refreshToken = {
                refreshCount++
            },
        ) {
            attempts++

            if (attempts == 1) {
                throw clientException(
                    HttpStatus.UNAUTHORIZED.value(),
                )
            }

            ResponseEntity.ok("success")
        }

        assertThat(result).isEqualTo("success")
        assertThat(attempts).isEqualTo(2)
        assertThat(refreshCount).isEqualTo(1)

        assertLog(
            status = ExternalApiCallStatus.SUCCESS,
            statusCode = 200,
            retryCount = 1,
        )
    }

    @Test
    fun `401이 반복돼도 토큰은 한 번만 갱신한다`() {
        var attempts = 0
        var refreshCount = 0

        assertThatThrownBy {
            execute(
                refreshToken = {
                    refreshCount++
                },
            ) {
                attempts++

                throw clientException(
                    HttpStatus.UNAUTHORIZED.value(),
                )
            }
        }.isInstanceOf(HttpClientErrorException::class.java)

        assertThat(attempts).isEqualTo(2)
        assertThat(refreshCount).isEqualTo(1)

        assertLog(
            status = ExternalApiCallStatus.FAIL,
            statusCode = 401,
            retryCount = 1,
        )
    }

    @Test
    fun `멱등하지 않은 요청은 401이 발생해도 토큰을 갱신하거나 재요청하지 않는다`() {
        var attempts = 0
        var refreshCount = 0

        assertThatThrownBy {
            execute(
                idempotent = false,
                refreshToken = {
                    refreshCount++
                },
            ) {
                attempts++

                throw clientException(
                    HttpStatus.UNAUTHORIZED.value(),
                )
            }
        }.isInstanceOf(HttpClientErrorException::class.java)

        assertThat(attempts).isEqualTo(1)
        assertThat(refreshCount).isZero()

        assertLog(
            status = ExternalApiCallStatus.FAIL,
            statusCode = 401,
            retryCount = 0,
        )
    }

    @Test
    fun `첫 요청의 401로 시도한 토큰 갱신이 실패하면 실패 로그를 저장하고 예외를 던진다`() {
        var attempts = 0
        var refreshCount = 0

        val refreshException =
            IllegalStateException("token refresh failed")

        assertThatThrownBy {
            execute(
                refreshToken = {
                    refreshCount++
                    throw refreshException
                },
            ) {
                attempts++

                throw clientException(
                    HttpStatus.UNAUTHORIZED.value(),
                )
            }
        }.isSameAs(refreshException)

        assertThat(attempts).isEqualTo(1)
        assertThat(refreshCount).isEqualTo(1)

        assertLog(
            status = ExternalApiCallStatus.FAIL,
            statusCode = 401,
            retryCount = 0,
        )

        assertThat(writer.savedLog?.errorMessage)
            .isEqualTo("token refresh failed")
    }

    @Test
    fun `401 토큰 갱신 후 5xx가 발생하면 네트워크 재시도를 수행한다`() {
        var attempts = 0
        var refreshCount = 0

        val result = execute(
            refreshToken = {
                refreshCount++
            },
        ) {
            attempts++

            when (attempts) {
                1 -> throw clientException(HttpStatus.UNAUTHORIZED.value())
                2 -> throw serverException(HttpStatus.INTERNAL_SERVER_ERROR.value())
                else -> ResponseEntity.ok("success")
            }
        }

        assertThat(result).isEqualTo("success")
        assertThat(attempts).isEqualTo(3)
        assertThat(refreshCount).isEqualTo(1)

        assertLog(
            status = ExternalApiCallStatus.SUCCESS,
            statusCode = 200,
            retryCount = 2,
        )
    }

    @Test
    fun `5xx 네트워크 재시도 후 401이 발생하면 토큰 갱신 재요청을 수행한다`() {
        var attempts = 0
        var refreshCount = 0

        val result = execute(
            refreshToken = {
                refreshCount++
            },
        ) {
            attempts++

            when (attempts) {
                1 -> throw serverException(HttpStatus.INTERNAL_SERVER_ERROR.value())
                2 -> throw clientException(HttpStatus.UNAUTHORIZED.value())
                else -> ResponseEntity.ok("success")
            }
        }

        assertThat(result).isEqualTo("success")
        assertThat(attempts).isEqualTo(3)
        assertThat(refreshCount).isEqualTo(1)

        assertLog(
            status = ExternalApiCallStatus.SUCCESS,
            statusCode = 200,
            retryCount = 2,
        )
    }

    @Test
    fun `멱등하지 않은 요청은 5xx가 발생해도 재시도하지 않는다`() {
        var attempts = 0

        assertThatThrownBy {
            execute(
                idempotent = false,
            ) {
                attempts++

                throw serverException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                )
            }
        }.isInstanceOf(HttpServerErrorException::class.java)

        assertThat(attempts).isEqualTo(1)

        assertLog(
            status = ExternalApiCallStatus.FAIL,
            statusCode = 500,
            retryCount = 0,
        )
    }

    @Test
    fun `API별 timeout과 retry 초기값이 정확하다`() {
        val expectedTimeouts =
            mapOf(
                ExternalApiCallPolicy.STOCK_PRICE to 5L,
                ExternalApiCallPolicy.NEWS_COLLECTION to 5L,
                ExternalApiCallPolicy.DISCLOSURE to 5L,
                ExternalApiCallPolicy.AI_CARD_NEWS to 15L,
            )

        expectedTimeouts.forEach { (policy, timeoutSeconds) ->
            assertThat(policy.timeout.seconds)
                .isEqualTo(timeoutSeconds)

            assertThat(policy.maxRetries)
                .isEqualTo(1)
        }
    }


    /***********************
    * Test Helping Methods
    ************************/

    // 공통 메서드를 만들었기 때문에 테스트에서는 요청 내용만 작성할 수 있음
    private fun execute(
        idempotent: Boolean = true,
        refreshToken: (() -> Unit)? = null,
        request: () -> ResponseEntity<String>,
    ): String {
        return service.execute(
            provider = "TEST_PROVIDER",
            apiName = "TEST_API",
            policy = ExternalApiCallPolicy.STOCK_PRICE,
            retryEnabled = idempotent,
            refreshToken = refreshToken,
            request = request,
        )
    }

    // 최종 외부 API 로그가 올바르게 저장됐는지 확인 (주요 필드만)
    private fun assertLog(
        status: ExternalApiCallStatus,
        statusCode: Int?,
        retryCount: Int,
    ) {
        val savedLog = checkNotNull(writer.savedLog)

        assertThat(savedLog.status).isEqualTo(status)
        assertThat(savedLog.responseStatusCode)
            .isEqualTo(statusCode)
        assertThat(savedLog.retryCount)
            .isEqualTo(retryCount)
    }

    // 테스트용 5xx 예외 생성
    private fun serverException(
        statusCode: Int,
    ): HttpServerErrorException {
        return HttpServerErrorException.create(
            HttpStatusCode.valueOf(statusCode),
            "server error",
            HttpHeaders.EMPTY,
            ByteArray(0),
            StandardCharsets.UTF_8,
        )
    }

    // 테스트용 4xx 예외 생성
    private fun clientException(
        statusCode: Int,
    ): HttpClientErrorException {
        return HttpClientErrorException.create(
            HttpStatusCode.valueOf(statusCode),
            "client error",
            HttpHeaders.EMPTY,
            ByteArray(0),
            StandardCharsets.UTF_8,
        )
    }

    // 실제 DB 저장은 기존 통합 테스트(log.service > IntegrationTest)에서 검증하므로, 여기서는 'savedLog 변수'에 보관하는 방식으로 대체한다.
    private class RecordingLogWriter : ExternalApiCallLogWriter {
        var savedLog: ExternalApiCallLog? = null
            private set

        override fun save(
            externalApiCallLog: ExternalApiCallLog, // 저장할 로그를 전달받아서
        ) {
            savedLog = externalApiCallLog // DB 대신 메모리 변수에 보관함 (=DB에 저장 형식 모사한 것)
        }
    }
}
