package com.brifo.server.auth.service

import com.brifo.server.auth.exception.GuestLoginRateLimitedException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

class GuestLoginRateLimiterTest {
    private val redisTemplate = mock(StringRedisTemplate::class.java)

    @Suppress("UNCHECKED_CAST")
    private val valueOperations = mock(ValueOperations::class.java) as ValueOperations<String, String>

    private val limiter = GuestLoginRateLimiter(redisTemplate)

    @Test
    fun `첫 요청은 카운터를 1로 만들고 윈도우 만료를 건다`() {
        `when`(redisTemplate.opsForValue()).thenReturn(valueOperations)
        `when`(valueOperations.increment(KEY)).thenReturn(1L)

        limiter.checkAndRecord(CLIENT_IP)

        verify(redisTemplate).expire(KEY, Duration.ofMinutes(10))
    }

    @Test
    fun `윈도우 안 재요청은 만료를 다시 걸지 않는다`() {
        `when`(redisTemplate.opsForValue()).thenReturn(valueOperations)
        `when`(valueOperations.increment(KEY)).thenReturn(2L)

        limiter.checkAndRecord(CLIENT_IP)

        verify(redisTemplate, never()).expire(anyString(), any(Duration::class.java))
    }

    @Test
    fun `제한 횟수를 넘으면 남은 시간과 함께 예외를 던진다`() {
        `when`(redisTemplate.opsForValue()).thenReturn(valueOperations)
        `when`(valueOperations.increment(KEY)).thenReturn(6L)
        `when`(redisTemplate.getExpire(KEY, TimeUnit.SECONDS)).thenReturn(120L)

        val exception = assertThrows<GuestLoginRateLimitedException> { limiter.checkAndRecord(CLIENT_IP) }

        assertEquals(120L, exception.retryAfterSeconds)
    }

    @Test
    fun `제한 횟수 이하이면 예외를 던지지 않는다`() {
        `when`(redisTemplate.opsForValue()).thenReturn(valueOperations)
        `when`(valueOperations.increment(KEY)).thenReturn(5L)

        limiter.checkAndRecord(CLIENT_IP)
    }

    private companion object {
        const val CLIENT_IP = "203.0.113.10"
        const val KEY = "guest-login:rate-limit:$CLIENT_IP"
    }
}
