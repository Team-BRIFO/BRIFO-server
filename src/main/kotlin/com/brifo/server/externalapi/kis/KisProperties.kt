package com.brifo.server.externalapi.kis

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "external.kis")
data class KisProperties(
    val baseUrl: String,
    val appKey: String,
    val appSecret: String,
)
