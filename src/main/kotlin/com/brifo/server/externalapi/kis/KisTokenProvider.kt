package com.brifo.server.externalapi.kis

import com.brifo.server.externalapi.kis.dto.KisTokenRequest
import com.brifo.server.externalapi.kis.dto.KisTokenResponse
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

    /**
     * 락을 못 잡으면 기다리지 않고 즉시 실패한다.
     *
     * 예전에는 락을 놓치면 몇 초씩 폴링했는데, 발급이 계속 실패하는 상황(예: 잘못된 키)에서는
     * 이 대기가 배치가 처리하는 종목 수만큼 그대로 곱해져 배치 하나가 수십 분씩 멈췄다.
     * 즉시 실패시키고 상위 재시도·다음 배치 실행에 맡기는 편이 훨씬 안전하다.
     */
    private fun issue(): String {
        if (!acquireIssueLock()) {
            return cachedToken()
                ?: error(
                    "KIS 토큰 발급이 진행 중이거나 방금 실패했습니다. " +
                        "발급은 1분에 1회로 제한되어 있어 잠시 후 다시 시도하면 됩니다.",
                )
        }

        // 여기서 실패하면 예외를 그대로 전파한다. 락은 일부러 유지한다 —
        // 지금 지우면 다음 호출이 곧바로 재시도해 KIS 분당 발급 제한을 다시 때린다.
        return requestAndCache()
    }

    private fun acquireIssueLock(): Boolean =
        redisTemplate.opsForValue().setIfAbsent(ISSUE_LOCK_KEY, LOCK_VALUE, ISSUE_INTERVAL) == true

    private fun requestAndCache(): String {
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
        const val LOCK_VALUE = "1"

        /** KIS 토큰 발급 제한 간격 (1분 1회) */
        val ISSUE_INTERVAL: Duration = Duration.ofMinutes(1)
        val EXPIRY_MARGIN: Duration = Duration.ofMinutes(5)
        val MIN_TTL: Duration = Duration.ofMinutes(1)
    }
}
