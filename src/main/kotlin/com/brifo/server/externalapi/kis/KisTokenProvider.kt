package com.brifo.server.externalapi.kis

import com.brifo.server.externalapi.kis.dto.KisTokenRequest
import com.brifo.server.externalapi.kis.dto.KisTokenResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.Duration

/**
 * KIS 접근 토큰을 Valkey에 캐싱한다.
 *
 * KIS는 토큰 발급을 **1분에 1회**로 제한하고 초과 시 `EGW00133`을 반환한다.
 * 인스턴스 메모리에만 두면 재시작·배포마다 재발급해 이 제한에 걸리고,
 * 그 순간 현재가·종가가 전부 실패한다. (종가는 정산 배치의 유일한 소스다.)
 */
@Component
@ConditionalOnProperty(prefix = "app.data-provider", name = ["type"], havingValue = "kis")
class KisTokenProvider(
    @Qualifier("kisCurrentPriceRestClient")
    private val restClient: RestClient,
    private val properties: KisProperties,
    private val redisTemplate: StringRedisTemplate,
) {
    fun getAccessToken(): String = cachedToken() ?: issue()

    /** 401 응답 후 강제 갱신 경로. 캐시된 토큰을 버리고 다시 발급받는다. */
    fun refresh(): String {
        redisTemplate.delete(TOKEN_KEY)
        return issue()
    }

    private fun cachedToken(): String? = redisTemplate.opsForValue().get(TOKEN_KEY)

    private fun issue(): String {
        // 인스턴스가 여러 대여도 1분에 한 번만 발급하도록 막는다.
        val acquired = redisTemplate.opsForValue().setIfAbsent(ISSUE_LOCK_KEY, "1", ISSUE_INTERVAL)

        if (acquired != true) {
            // 다른 인스턴스가 방금 발급했다면 그 토큰을 그대로 쓴다.
            cachedToken()?.let { return it }
            // 캐시가 비어 있으면 EGW00133을 감수하고 시도한다. 실패해도 조용히 죽는 것보다 낫다.
            log.warn("KIS 토큰 발급 간격 제한 중이지만 캐시가 비어 있어 발급을 시도합니다.")
        }

        val response = restClient
            .post()
            .uri("/oauth2/tokenP")
            .body(
                KisTokenRequest(
                    appKey = properties.appKey,
                    appSecret = properties.appSecret,
                ),
            )
            .retrieve()
            .body(KisTokenResponse::class.java)
            ?: error("KIS token response body is empty")

        // 만료 직전에 갱신되도록 여유를 두고 TTL을 잡는다.
        val ttlSeconds = (response.expiresIn - EXPIRY_MARGIN.seconds).coerceAtLeast(MIN_TTL.seconds)
        redisTemplate.opsForValue().set(TOKEN_KEY, response.accessToken, Duration.ofSeconds(ttlSeconds))

        return response.accessToken
    }

    private companion object {
        const val TOKEN_KEY = "kis:access-token"
        const val ISSUE_LOCK_KEY = "kis:access-token:issue-lock"

        /** KIS 토큰 발급 제한 간격 (1분 1회) */
        val ISSUE_INTERVAL: Duration = Duration.ofMinutes(1)
        val EXPIRY_MARGIN: Duration = Duration.ofMinutes(5)
        val MIN_TTL: Duration = Duration.ofMinutes(1)

        val log = LoggerFactory.getLogger(KisTokenProvider::class.java)
    }
}
