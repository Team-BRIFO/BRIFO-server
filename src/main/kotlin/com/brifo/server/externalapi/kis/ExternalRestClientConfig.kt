package com.brifo.server.externalapi.kis

import com.brifo.server.externalapi.ExternalApiCallPolicy
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient

@Configuration
@ConditionalOnProperty(prefix = "app.data-provider", name = ["type"], havingValue = "kis")
@EnableConfigurationProperties(KisProperties::class)
class ExternalRestClientConfig {
    @Bean("kisCurrentPriceRestClient")
    fun kisCurrentPriceRestClient(
        properties: KisProperties,
    ): RestClient {
        val policy = ExternalApiCallPolicy.STOCK_PRICE

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
        val policy = ExternalApiCallPolicy.STOCK_PRICE

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
