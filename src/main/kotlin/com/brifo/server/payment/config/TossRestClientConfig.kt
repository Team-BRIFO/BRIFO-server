package com.brifo.server.payment.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.util.Base64

@Configuration
@EnableConfigurationProperties(TossPaymentsProperties::class)
class TossRestClientConfig {
    @Bean("tossPaymentsRestClient")
    fun tossPaymentsRestClient(properties: TossPaymentsProperties): RestClient {
        // 토스페이먼츠는 시크릿 키를 Basic 인증의 username으로, 비밀번호는 빈 값으로 사용한다.
        val credentials = Base64.getEncoder().encodeToString("${properties.secretKey}:".toByteArray())
        val requestFactory =
            SimpleClientHttpRequestFactory().apply {
                setConnectTimeout(properties.connectTimeout)
                setReadTimeout(properties.readTimeout)
            }
        return RestClient.builder()
            .baseUrl(properties.baseUrl)
            .defaultHeader("Authorization", "Basic $credentials")
            .requestFactory(requestFactory)
            .build()
    }
}
