package com.brifo.server.briefing.client

import com.brifo.server.externalapi.ExternalApiCallPolicy
import com.brifo.server.externalapi.ai.AiProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient

@Configuration
class AiBriefingRestClientConfig {
    @Bean("aiBriefingRestClient")
    fun aiBriefingRestClient(
        properties: AiProperties,
    ): RestClient {
        val policy = ExternalApiCallPolicy.AI_BRIEFING

        // 브리핑 전용 timeout을 적용한다.
        val requestFactory =
            SimpleClientHttpRequestFactory().apply {
                setConnectTimeout(policy.timeout)
                setReadTimeout(policy.timeout)
            }

        // 기본 URL과 AI 내부 API 인증 헤더를 설정한다.
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
