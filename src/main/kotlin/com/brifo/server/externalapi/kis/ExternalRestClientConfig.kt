package com.brifo.server.externalapi.kis

import com.brifo.server.externalapi.ExternalApiCallPolicy
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient

@Configuration
@EnableConfigurationProperties(KisProperties::class)
class ExternalRestClientConfig {
    @Bean("kisCurrentPriceRestClient")
    fun kisCurrentPriceRestClient(
        properties: KisProperties,
    ): RestClient {
        val policy = ExternalApiCallPolicy.KIS_CURRENT_PRICE

        val requestFactory =
            SimpleClientHttpRequestFactory().apply {
                setConnectTimeout(policy.timeout)
                setReadTimeout(policy.timeout)
            }

        return RestClient
            .builder()
            .baseUrl(properties.baseUrl)
            .requestFactory(requestFactory)
            .build()
    }

    @Bean("kisDailyPriceRestClient")
    fun kisDailyPriceRestClient(
        properties: KisProperties,
    ): RestClient {
        val policy = ExternalApiCallPolicy.KIS_CLOSING_PRICE

        val requestFactory =
            SimpleClientHttpRequestFactory().apply {
                setConnectTimeout(policy.timeout)
                setReadTimeout(policy.timeout)
            }

        return RestClient
            .builder()
            .baseUrl(properties.baseUrl)
            .requestFactory(requestFactory)
            .build()
    }
}
