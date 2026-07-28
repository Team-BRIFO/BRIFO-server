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
) {
    fun save(price: StockCurrentPriceCache) {
        val key = "stock:current-price:${price.code}"
        val value = objectMapper.writeValueAsString(price)

        redisTemplate.opsForValue().set(key, value, cacheTtl)
    }

    fun findByCode(code: String): StockCurrentPriceCache? {
        val key = "stock:current-price:$code"
        val value = redisTemplate.opsForValue().get(key) ?: return null

        return objectMapper.readValue(
            value,
            StockCurrentPriceCache::class.java,
        )
    }
}
