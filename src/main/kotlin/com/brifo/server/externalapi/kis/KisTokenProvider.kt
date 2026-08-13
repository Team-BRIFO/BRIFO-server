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
    /** 다른 인스턴스가 발급을 끝낼 때까지 기다리는 시간. 테스트에서 줄여 쓴다. */
    private val issueWaitTimeout: Duration = Duration.ofSeconds(5),
    private val pollInterval: Duration = Duration.ofMillis(200),
) {
    fun getAccessToken(): String = cachedToken() ?: issue()

    /** 401 응답 후 강제 갱신 경로. 캐시된 토큰을 버리고 다시 발급받는다. */
    fun refresh(): String {
        redisTemplate.delete(TOKEN_KEY)
        return issue()
    }

    private fun cachedToken(): String? = redisTemplate.opsForValue().get(TOKEN_KEY)

    /**
     * 발급 락을 잡은 인스턴스만 KIS를 호출한다.
     *
     * 락을 놓친 인스턴스가 곧바로 발급을 시도하면, 락을 잡은 쪽이 아직 토큰을 저장하기 전인
     * 짧은 구간에서 동시에 KIS를 때려 분당 제한을 다시 초과한다.
     * 그래서 락을 놓치면 토큰이 캐시에 나타날 때까지 기다린다.
     */
    private fun issue(): String {
        repeat(MAX_ISSUE_ROUNDS) { round ->
            if (acquireIssueLock()) {
                return requestAndCache()
            }

            // 락을 잡은 인스턴스가 저장을 끝내길 기다린다.
            awaitCachedToken()?.let { return it }

            // 여기까지 왔다면 락 보유자가 발급에 실패했거나 죽은 것이다.
            // 락 TTL이 만료되면 다음 라운드에서 우리가 락을 잡는다.
            log.warn(
                "KIS 토큰 대기 시간을 초과했습니다. 재시도합니다. round={}/{}",
                round + 1,
                MAX_ISSUE_ROUNDS,
            )
        }

        error("KIS 토큰을 확보하지 못했습니다. 발급 락이 해제되지 않았습니다.")
    }

    private fun acquireIssueLock(): Boolean =
        redisTemplate.opsForValue().setIfAbsent(ISSUE_LOCK_KEY, LOCK_VALUE, ISSUE_INTERVAL) == true

    /** 캐시에 토큰이 나타날 때까지 짧게 폴링한다. 시간 내에 못 받으면 null. */
    private fun awaitCachedToken(): String? {
        val deadline = System.nanoTime() + issueWaitTimeout.toNanos()

        while (System.nanoTime() < deadline) {
            cachedToken()?.let { return it }

            try {
                Thread.sleep(pollInterval.toMillis())
            } catch (exception: InterruptedException) {
                Thread.currentThread().interrupt()
                throw IllegalStateException("KIS 토큰 대기 중 인터럽트되었습니다.", exception)
            }
        }

        return cachedToken()
    }

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

        /**
         * 락 보유자가 죽었을 때를 대비한 재시도 횟수.
         * 요청 경로에서 호출되므로 무한정 기다리지 않고, 실패 시 상위의 재시도·폴백에 맡긴다.
         */
        const val MAX_ISSUE_ROUNDS = 2

        val log = LoggerFactory.getLogger(KisTokenProvider::class.java)
    }
}
