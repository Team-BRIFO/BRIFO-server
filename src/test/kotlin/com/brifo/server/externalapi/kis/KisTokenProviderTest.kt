package com.brifo.server.externalapi.kis

import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.web.client.RestClient
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class KisTokenProviderTest {
    private val restClient = mock(RestClient::class.java)

    private val properties =
        KisProperties(
            baseUrl = "https://openapi.koreainvestment.com:9443",
            appKey = "test-app-key",
            appSecret = "test-app-secret",
        )

    @Test
    fun `캐시된 토큰이 있으면 재발급하지 않는다`() {
        val redisTemplate = fakeStringRedisTemplate(ConcurrentHashMap(mapOf(TOKEN_KEY to "cached-token")))

        assertEquals(
            "cached-token",
            KisTokenProvider(restClient, properties, redisTemplate).getAccessToken(),
        )
        verify(restClient, never()).post()
    }

    @Test
    fun `락을 못 잡고 캐시도 비어 있으면 기다리지 않고 즉시 실패한다`() {
        // 다른 호출자가 발급 중이거나 방금 실패한 상황.
        // 예전에는 여기서 몇 초씩 폴링했는데, 발급이 계속 실패하면 그 대기가
        // 배치가 처리하는 종목 수만큼 곱해져 배치 전체가 수십 분씩 멈췄다.
        val redisTemplate = redisTemplateWithLock(acquired = false, cachedToken = null)

        val exception =
            assertFailsWith<IllegalStateException> {
                KisTokenProvider(restClient, properties, redisTemplate).getAccessToken()
            }

        assertTrue(exception.message!!.contains("1분에 1회"))
        verify(restClient, never()).post()
    }

    @Test
    fun `락을 못 잡아도 캐시에 토큰이 있으면 그걸 쓴다`() {
        val redisTemplate = redisTemplateWithLock(acquired = false, cachedToken = "token-from-other-instance")

        assertEquals(
            "token-from-other-instance",
            KisTokenProvider(restClient, properties, redisTemplate).getAccessToken(),
        )
        verify(restClient, never()).post()
    }

    @Test
    fun `락을 잡았는데 발급이 실패하면 예외를 그대로 전파하고 락은 유지한다`() {
        // 락을 지우면 바로 다음 호출자가 재시도해 KIS 분당 발급 제한을 다시 때린다.
        val redisTemplate = redisTemplateWithLock(acquired = true, cachedToken = null)
        `when`(restClient.post()).thenThrow(RuntimeException("KIS unavailable"))

        assertFailsWith<RuntimeException> {
            KisTokenProvider(restClient, properties, redisTemplate).getAccessToken()
        }

        verify(redisTemplate, never()).delete(ISSUE_LOCK_KEY)
    }

    @Test
    fun `강제 갱신은 캐시를 비우고, 락을 못 잡으면 최신 캐시값을 그대로 쓴다`() {
        val redisTemplate = redisTemplateWithLock(acquired = false, cachedToken = "renewed-token")

        assertEquals("renewed-token", KisTokenProvider(restClient, properties, redisTemplate).refresh())
        verify(redisTemplate).delete(TOKEN_KEY)
        verify(restClient, never()).post()
    }

    private fun redisTemplateWithLock(
        acquired: Boolean,
        cachedToken: String?,
    ): StringRedisTemplate {
        val redisTemplate = mock(StringRedisTemplate::class.java)

        @Suppress("UNCHECKED_CAST")
        val valueOperations = mock(ValueOperations::class.java) as ValueOperations<String, String>

        `when`(redisTemplate.opsForValue()).thenReturn(valueOperations)
        `when`(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration::class.java)))
            .thenReturn(acquired)
        `when`(valueOperations.get(TOKEN_KEY)).thenReturn(cachedToken)

        return redisTemplate
    }

    private companion object {
        const val TOKEN_KEY = "kis:access-token"
        const val ISSUE_LOCK_KEY = "kis:access-token:issue-lock"
    }
}
