package com.brifo.server.stock.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(StockPriceProperties::class)
class StockPriceConfiguration
