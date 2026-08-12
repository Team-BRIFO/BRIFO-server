package com.brifo.server.externalapi.naver

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient

@Configuration
@EnableConfigurationProperties(NaverNewsProperties::class)
class NaverRestClientConfig {
    @Bean("naverNewsRestClient")
    fun naverNewsRestClient(properties: NaverNewsProperties): RestClient {
        val requestFactory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(properties.connectTimeout)
            setReadTimeout(properties.readTimeout)
        }
        return RestClient.builder()
            .baseUrl(properties.baseUrl)
            .defaultHeader("X-NCP-APIGW-API-KEY-ID", properties.clientId)
            .defaultHeader("X-NCP-APIGW-API-KEY", properties.clientSecret)
            .requestFactory(requestFactory)
            .build()
    }
}
