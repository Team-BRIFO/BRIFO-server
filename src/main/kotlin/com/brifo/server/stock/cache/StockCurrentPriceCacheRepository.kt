package com.brifo.server.stock.cache

import com.brifo.server.stock.config.StockPriceProperties
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository

@Repository
class StockCurrentPriceCacheRepository(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
    private val properties: StockPriceProperties,
) {
    fun save(price: StockCurrentPriceCache) {
        val key = "stock:delayed-price:${price.code}"
        val value = objectMapper.writeValueAsString(price)

        redisTemplate.opsForValue().set(
            key,
            value,
            properties.cacheTtl,
        )
    }

    fun saveLastSuccess(price: StockCurrentPriceCache) {
        val key = "stock:last-success-price:${price.code}"
        val value = objectMapper.writeValueAsString(price)

        redisTemplate.opsForValue().set(
            key,
            value,
            properties.lastSuccessTtl,
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
