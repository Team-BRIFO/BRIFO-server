package com.brifo.server.externalapi.kis

import org.springframework.boot.context.properties.ConfigurationProperties

// KIS API 호출에 필요한 설정값
@ConfigurationProperties(prefix = "external.kis")
data class KisProperties(
    val baseUrl: String,
    val appKey: String,
    val appSecret: String,
)
