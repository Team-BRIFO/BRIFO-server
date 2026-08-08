package com.brifo.server.stock.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("app.stock-price")
data class StockPriceProperties(
    val cacheTtl: Duration,
    val lastSuccessTtl: Duration,
)
