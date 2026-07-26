package com.brifo.server.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.time.Duration

@Configuration
class OAuthClientConfig {
    @Bean
    fun kakaoRestClient(properties: KakaoProperties): RestClient = createRestClient(properties.connectTimeout, properties.readTimeout)

    @Bean
    fun naverRestClient(properties: NaverProperties): RestClient = createRestClient(properties.connectTimeout, properties.readTimeout)

    private fun createRestClient(
        connectTimeout: Duration,
        readTimeout: Duration,
    ): RestClient {
        val requestFactory =
            SimpleClientHttpRequestFactory().apply {
                setConnectTimeout(connectTimeout)
                setReadTimeout(readTimeout)
            }

        return RestClient.builder().requestFactory(requestFactory).build()
    }
}
