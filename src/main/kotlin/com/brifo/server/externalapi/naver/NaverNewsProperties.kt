package com.brifo.server.externalapi.naver

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "external.naver")
data class NaverNewsProperties(
    val baseUrl: String = "https://naverapihub.apigw.ntruss.com",
    val clientId: String = "disabled",
    val clientSecret: String = "disabled",
    val connectTimeout: Duration = Duration.ofSeconds(3),
    val readTimeout: Duration = Duration.ofSeconds(5),
    val displayCount: Int = 20,
)
