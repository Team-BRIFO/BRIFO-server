package com.brifo.server.stock.cache

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository
import java.time.Duration

@Repository
class StockCurrentPriceCacheRepository(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
    @Value("\${app.stock-price.cache-ttl}")
    private val cacheTtl: Duration,
    @Value("\${app.stock-price.last-success-ttl}")
    private val lastSuccessTtl: Duration,
) {
    // 현재가를 60초 캐시에 저장한다.
    fun save(price: StockCurrentPriceCache) {
        val key = "stock:delayed-price:${price.code}"
        val value = objectMapper.writeValueAsString(price)

        redisTemplate.opsForValue().set(
            key,
            value,
            cacheTtl,
        )
    }

    // 마지막 성공값을 3분 캐시에 저장한다.
    fun saveLastSuccess(price: StockCurrentPriceCache) {
        val key = "stock:last-success-price:${price.code}"
        val value = objectMapper.writeValueAsString(price)

        redisTemplate.opsForValue().set(
            key,
            value,
            lastSuccessTtl,
        )
    }

    // 현재가 캐시를 조회한다.
    fun findByCode(code: String): StockCurrentPriceCache? {
        val key = "stock:delayed-price:$code"
        val value =
            redisTemplate.opsForValue().get(key)
                ?: return null

        return objectMapper.readValue(
            value,
            StockCurrentPriceCache::class.java,
        )
    }

    // 마지막 성공값 캐시를 조회한다.
    fun findLastSuccessByCode(code: String): StockCurrentPriceCache? {
        val key = "stock:last-success-price:$code"
        val value =
            redisTemplate.opsForValue().get(key)
                ?: return null

        return objectMapper.readValue(
            value,
            StockCurrentPriceCache::class.java,
        )
    }
}
