package com.brifo.server.stock.cache

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDateTime

class StockCurrentPriceCacheRepositoryIntegrationTest {
    private lateinit var connectionFactory:
        LettuceConnectionFactory

    private lateinit var redisTemplate:
        StringRedisTemplate

    private lateinit var repository:
        StockCurrentPriceCacheRepository

    @BeforeEach
    fun setUp() {
        val host =
            System.getenv("REDIS_HOST")
                ?: "localhost"

        val port =
            System.getenv("REDIS_PORT")
                ?.toInt()
                ?: 6379

        // 현재 실행 중인 Valkey에 연결한다.
        connectionFactory =
            LettuceConnectionFactory(
                host,
                port,
            ).apply {
                afterPropertiesSet()
            }

        redisTemplate =
            StringRedisTemplate(
                connectionFactory,
            ).apply {
                afterPropertiesSet()
            }

        repository =
            StockCurrentPriceCacheRepository(
                redisTemplate = redisTemplate,
                objectMapper =
                    jacksonObjectMapper()
                        .findAndRegisterModules(),
                cacheTtl = Duration.ofSeconds(60),
                lastSuccessTtl = Duration.ofMinutes(3),
            )

        // 테스트 전용 캐시를 비운다.
        redisTemplate.delete(CURRENT_PRICE_KEY)
        redisTemplate.delete(LAST_SUCCESS_KEY)
    }

    @AfterEach
    fun tearDown() {
        // 테스트가 만든 캐시만 삭제한다.
        redisTemplate.delete(CURRENT_PRICE_KEY)
        redisTemplate.delete(LAST_SUCCESS_KEY)

        connectionFactory.destroy()
    }

    @Test
    fun `현재가를 실제 Valkey에 60초간 저장하고 조회한다`() {
        val price =
            StockCurrentPriceCache(
                code = STOCK_CODE,
                currentPrice = BigDecimal("79800"),
                priceChange = BigDecimal("5900"),
                changeRate = BigDecimal("8.1"),
                fetchedAt =
                    LocalDateTime.of(
                        2026,
                        7,
                        29,
                        10,
                        0,
                    ),
            )

        repository.save(price)

        val savedPrice =
            repository.findByCode(STOCK_CODE)

        val ttl =
            redisTemplate.getExpire(
                CURRENT_PRICE_KEY,
            )

        // 실제 Valkey에서 같은 값을 조회한다.
        assertThat(savedPrice)
            .isEqualTo(price)

        // 현재가 TTL은 최대 60초다.
        assertThat(ttl)
            .isBetween(1L, 60L)
    }

    @Test
    fun `마지막 성공값을 실제 Valkey에 3분간 저장하고 조회한다`() {
        val price =
            StockCurrentPriceCache(
                code = STOCK_CODE,
                currentPrice = BigDecimal("79700"),
                priceChange = BigDecimal("5800"),
                changeRate = BigDecimal("7.9"),
                fetchedAt =
                    LocalDateTime.of(
                        2026,
                        7,
                        29,
                        9,
                        59,
                    ),
            )

        repository.saveLastSuccess(price)

        val savedPrice =
            repository.findLastSuccessByCode(
                STOCK_CODE,
            )

        val ttl =
            redisTemplate.getExpire(
                LAST_SUCCESS_KEY,
            )

        // 실제 Valkey에서 같은 값을 조회한다.
        assertThat(savedPrice)
            .isEqualTo(price)

        // 마지막 성공값 TTL은 최대 3분이다.
        assertThat(ttl)
            .isBetween(1L, 180L)
    }

    companion object {
        // 실제 종목 캐시와 겹치지 않는 테스트 코드다.
        private const val STOCK_CODE =
            "TEST-CACHE"

        private const val CURRENT_PRICE_KEY =
            "stock:delayed-price:$STOCK_CODE"

        private const val LAST_SUCCESS_KEY =
            "stock:last-success-price:$STOCK_CODE"
    }
}
