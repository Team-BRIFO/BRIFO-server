package com.brifo.server.news.client

import com.brifo.server.externalapi.ExternalApiCallPolicy
import com.brifo.server.externalapi.ai.AiProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient

@Configuration
class AiNewsCardRestClientConfig {
    @Bean("aiNewsCardRestClient")
    fun aiNewsCardRestClient(
        properties: AiProperties,
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
            .baseUrl(properties.baseUrl)
            .defaultHeader(AI_INTERNAL_API_KEY_HEADER, properties.apiKey)
            .requestFactory(requestFactory)
            .build()
    }

    private companion object {
        const val AI_INTERNAL_API_KEY_HEADER = "AI_INTERNAL_API_KEY"
    }
}
