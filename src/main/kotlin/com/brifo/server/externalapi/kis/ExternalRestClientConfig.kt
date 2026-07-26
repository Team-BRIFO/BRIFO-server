package com.brifo.server.externalapi.kis

import com.brifo.server.externalapi.ExternalApiCallPolicy
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient

@Configuration
class ExternalRestClientConfig {
    @Bean("kisCurrentPriceRestClient")
    fun kisCurrentPriceRestClient(
        @Value("\${external.kis.base-url}")
        baseUrl: String,
    ): RestClient {
        val policy =
            ExternalApiCallPolicy.KIS_CURRENT_PRICE

        val requestFactory =
            SimpleClientHttpRequestFactory().apply {
                setConnectTimeout(policy.timeout)
                setReadTimeout(policy.timeout)
            }

        return RestClient
            .builder()
            .baseUrl(baseUrl)
            .requestFactory(requestFactory)
            .build()
    }
}
