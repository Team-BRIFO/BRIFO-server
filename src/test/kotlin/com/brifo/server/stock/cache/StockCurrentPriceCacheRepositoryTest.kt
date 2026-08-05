package com.brifo.server.stock.cache

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDateTime
import kotlin.test.assertNull

class StockCurrentPriceCacheRepositoryTest {
    private val redisTemplate = mock(StringRedisTemplate::class.java)
    private val objectMapper = mock(ObjectMapper::class.java)

    @Suppress("UNCHECKED_CAST")
    private val valueOperations =
        mock(ValueOperations::class.java) as ValueOperations<String, String>

    private val repository =
        StockCurrentPriceCacheRepository(
            redisTemplate = redisTemplate,
            objectMapper = objectMapper,
            cacheTtl = Duration.ofSeconds(60),
            lastSuccessTtl = Duration.ofMinutes(3),
        )

    @Test
    fun `현재가를 종목별 key와 TTL 60초로 저장한다`() {
        val price =
            StockCurrentPriceCache(
                code = "005930",
                currentPrice = BigDecimal("79800"),
                priceChange = BigDecimal("5900"),
                changeRate = BigDecimal("8.1"),
                fetchedAt = LocalDateTime.of(2026, 7, 28, 10, 15),
            )

        `when`(redisTemplate.opsForValue())
            .thenReturn(valueOperations)
        `when`(objectMapper.writeValueAsString(price))
            .thenReturn("""{"code":"005930"}""")

        repository.save(price)

        verify(valueOperations).set(
            "stock:delayed-price:005930",
            """{"code":"005930"}""",
            Duration.ofSeconds(60),
        )
    }

    @Test
    fun `마지막 성공값을 3분간 저장한다`() {
        val price =
            StockCurrentPriceCache(
                code = "005930",
                currentPrice = BigDecimal("79800"),
                priceChange = BigDecimal("5900"),
                changeRate = BigDecimal("8.1"),
                fetchedAt = LocalDateTime.of(2026, 7, 28, 10, 15),
            )

        `when`(redisTemplate.opsForValue())
            .thenReturn(valueOperations)
        `when`(objectMapper.writeValueAsString(price))
            .thenReturn("""{"code":"005930"}""")

        repository.saveLastSuccess(price)

        verify(valueOperations).set(
            "stock:last-success-price:005930",
            """{"code":"005930"}""",
            Duration.ofMinutes(3),
        )
    }

    @Test
    fun `캐시가 없으면 null을 반환한다`() {
        `when`(redisTemplate.opsForValue())
            .thenReturn(valueOperations)
        `when`(valueOperations.get("stock:delayed-price:005930"))
            .thenReturn(null)

        val result = repository.findByCode("005930")

        assertNull(result)
    }
}
