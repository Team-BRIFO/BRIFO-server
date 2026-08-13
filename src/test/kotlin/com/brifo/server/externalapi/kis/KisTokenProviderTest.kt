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
import kotlin.test.assertEquals

class KisTokenProviderTest {
    private val restClient = mock(RestClient::class.java)
    private val redisTemplate = mock(StringRedisTemplate::class.java)

    @Suppress("UNCHECKED_CAST")
    private val valueOperations = mock(ValueOperations::class.java) as ValueOperations<String, String>

    private val properties =
        KisProperties(
            baseUrl = "https://openapi.koreainvestment.com:9443",
            appKey = "test-app-key",
            appSecret = "test-app-secret",
        )

    private val provider by lazy { KisTokenProvider(restClient, properties, redisTemplate) }

    @Test
    fun `캐시된 토큰이 있으면 재발급하지 않는다`() {
        // KIS는 1분에 1회만 발급을 허용하므로 캐시가 살아 있는 동안은 호출하면 안 된다.
        `when`(redisTemplate.opsForValue()).thenReturn(valueOperations)
        `when`(valueOperations.get(TOKEN_KEY)).thenReturn("cached-token")

        assertEquals("cached-token", provider.getAccessToken())
        verify(restClient, never()).post()
    }

    @Test
    fun `강제 갱신은 캐시를 비우고 발급 간격 제한을 지킨다`() {
        `when`(redisTemplate.opsForValue()).thenReturn(valueOperations)
        // 발급 락을 못 잡은 상황 = 1분 안에 다른 인스턴스가 이미 발급함
        `when`(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration::class.java)))
            .thenReturn(false)
        `when`(valueOperations.get(TOKEN_KEY)).thenReturn("token-from-other-instance")

        assertEquals("token-from-other-instance", provider.refresh())
        verify(redisTemplate).delete(TOKEN_KEY)
        verify(restClient, never()).post()
    }

    private companion object {
        const val TOKEN_KEY = "kis:access-token"
    }
}
