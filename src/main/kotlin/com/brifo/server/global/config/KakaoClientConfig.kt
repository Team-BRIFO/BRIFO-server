package com.brifo.server.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient

@Configuration
class KakaoClientConfig {
    @Bean
    fun kakaoRestClient(properties: KakaoProperties): RestClient {
        val requestFactory =
            SimpleClientHttpRequestFactory().apply {
                setConnectTimeout(properties.connectTimeout)
                setReadTimeout(properties.readTimeout)
            }

        return RestClient.builder().requestFactory(requestFactory).build()
    }
}
