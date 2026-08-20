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
    /** 종목당 수집할 뉴스 건수. 종목당 호출은 1회라 건수를 바꿔도 API 호출 수는 그대로다. */
    val displayCount: Int = 3,
)
