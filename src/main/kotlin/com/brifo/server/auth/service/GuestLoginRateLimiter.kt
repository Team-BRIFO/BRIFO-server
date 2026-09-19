package com.brifo.server.auth.service

import com.brifo.server.auth.exception.GuestLoginRateLimitedException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * 게스트 로그인은 인증 없이 누구나 호출할 수 있어, 반복 호출만으로 계정과 AP 거래가
 * 무한정 쌓일 수 있다(DoS 여지). IP당 고정 윈도우로 요청 횟수를 제한해 막는다.
 * 여러 서버 인스턴스에서도 동일하게 동작하도록 카운터를 Redis에 둔다.
 */
@Component
class GuestLoginRateLimiter(
    private val redisTemplate: StringRedisTemplate,
) {
    fun checkAndRecord(clientIp: String) {
        val key = "$KEY_PREFIX$clientIp"
        val count = redisTemplate.opsForValue().increment(key) ?: 1L
        if (count == 1L) {
            redisTemplate.expire(key, WINDOW)
        }
        if (count > LIMIT) {
            val retryAfterSeconds = redisTemplate.getExpire(key, TimeUnit.SECONDS).coerceAtLeast(1L)
            throw GuestLoginRateLimitedException(retryAfterSeconds)
        }
    }

    private companion object {
        const val KEY_PREFIX = "guest-login:rate-limit:"
        const val LIMIT = 5L
        val WINDOW: Duration = Duration.ofMinutes(10)
    }
}
