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

    private fun provider(redisTemplate: StringRedisTemplate) =
        KisTokenProvider(
            restClient = restClient,
            properties = properties,
            redisTemplate = redisTemplate,
            // 테스트가 오래 걸리지 않도록 대기 시간을 줄인다.
            issueWaitTimeout = Duration.ofMillis(300),
            pollInterval = Duration.ofMillis(20),
        )

    @Test
    fun `캐시된 토큰이 있으면 재발급하지 않는다`() {
        // KIS는 1분에 1회만 발급을 허용하므로 캐시가 살아 있는 동안은 호출하면 안 된다.
        val store = ConcurrentHashMap(mapOf(TOKEN_KEY to "cached-token"))

        assertEquals("cached-token", provider(fakeStringRedisTemplate(store)).getAccessToken())
        verify(restClient, never()).post()
    }

    @Test
    fun `락을 놓치면 다른 인스턴스가 토큰을 저장할 때까지 기다린다`() {
        // 락을 잡은 인스턴스가 아직 토큰을 저장하기 전인 구간.
        // 여기서 곧바로 발급하면 분당 제한을 다시 초과한다.
        val redisTemplate = mock(StringRedisTemplate::class.java)

        @Suppress("UNCHECKED_CAST")
        val valueOperations = mock(ValueOperations::class.java) as ValueOperations<String, String>

        `when`(redisTemplate.opsForValue()).thenReturn(valueOperations)
        `when`(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration::class.java)))
            .thenReturn(false)
        // 처음 두 번은 아직 저장 전, 세 번째에 다른 인스턴스가 저장을 끝낸다.
        `when`(valueOperations.get(TOKEN_KEY))
            .thenReturn(null, null, "token-from-other-instance")

        assertEquals("token-from-other-instance", provider(redisTemplate).getAccessToken())
        verify(restClient, never()).post()
    }

    @Test
    fun `락 보유자가 토큰을 끝내 저장하지 않으면 발급하지 않고 실패한다`() {
        // 락은 계속 잡혀 있는데 토큰이 안 나타나는 상황.
        // 무작정 발급하면 EGW00133을 유발하므로, 상위의 재시도·폴백에 맡기고 실패한다.
        val redisTemplate = mock(StringRedisTemplate::class.java)

        @Suppress("UNCHECKED_CAST")
        val valueOperations = mock(ValueOperations::class.java) as ValueOperations<String, String>

        `when`(redisTemplate.opsForValue()).thenReturn(valueOperations)
        `when`(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration::class.java)))
            .thenReturn(false)
        `when`(valueOperations.get(TOKEN_KEY)).thenReturn(null)

        val exception =
            assertFailsWith<IllegalStateException> {
                provider(redisTemplate).getAccessToken()
            }

        assertTrue(exception.message!!.contains("발급 락"))
        verify(restClient, never()).post()
    }

    @Test
    fun `강제 갱신은 캐시를 비운 뒤 다른 인스턴스의 새 토큰을 받는다`() {
        val redisTemplate = mock(StringRedisTemplate::class.java)

        @Suppress("UNCHECKED_CAST")
        val valueOperations = mock(ValueOperations::class.java) as ValueOperations<String, String>

        `when`(redisTemplate.opsForValue()).thenReturn(valueOperations)
        // 락은 다른 인스턴스가 쥐고 있다.
        `when`(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration::class.java)))
            .thenReturn(false)
        // 캐시를 비운 직후엔 비어 있고, 곧 다른 인스턴스가 새 토큰을 저장한다.
        `when`(valueOperations.get(TOKEN_KEY)).thenReturn(null, "renewed-token")

        assertEquals("renewed-token", provider(redisTemplate).refresh())
        verify(redisTemplate).delete(TOKEN_KEY)
        verify(restClient, never()).post()
    }

    private companion object {
        const val TOKEN_KEY = "kis:access-token"
    }
}
