package com.brifo.server.news.client

import com.brifo.server.externalapi.ExternalApiCallPolicy
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient

@Configuration
class AiNewsCardRestClientConfig {
    @Bean("aiNewsCardRestClient")
    fun aiNewsCardRestClient(
        @Value("\${external.ai.base-url:disabled}")
        baseUrl: String,
        @Value("\${external.ai.api-key:disabled}")
        apiKey: String,
    ): RestClient {
        val policy = ExternalApiCallPolicy.AI_CARD_NEWS

        // 기존 FastAPI timeout 정책을 사용한다.
        val requestFactory =
            SimpleClientHttpRequestFactory().apply {
                setConnectTimeout(policy.timeout)
                setReadTimeout(policy.timeout)
            }

        return RestClient
            .builder()
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer $apiKey")
            .requestFactory(requestFactory)
            .build()
    }
}
