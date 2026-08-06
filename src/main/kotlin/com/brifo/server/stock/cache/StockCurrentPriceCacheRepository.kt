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
    fun save(price: StockCurrentPriceCache) {
        val key = "stock:delayed-price:${price.code}"
        val value = objectMapper.writeValueAsString(price)

        redisTemplate.opsForValue().set(
            key,
            value,
            cacheTtl,
        )
    }

    fun saveLastSuccess(price: StockCurrentPriceCache) {
        val key = "stock:last-success-price:${price.code}"
        val value = objectMapper.writeValueAsString(price)

        redisTemplate.opsForValue().set(
            key,
            value,
            lastSuccessTtl,
        )
    }

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
