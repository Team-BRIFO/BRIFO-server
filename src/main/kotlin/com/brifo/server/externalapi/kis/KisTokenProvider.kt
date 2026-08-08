package com.brifo.server.externalapi.kis

import com.brifo.server.externalapi.kis.dto.KisTokenRequest
import com.brifo.server.externalapi.kis.dto.KisTokenResponse
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.Instant

@Component
@ConditionalOnProperty(prefix = "app.stock-price", name = ["provider"], havingValue = "kis")
class KisTokenProvider(
    @Qualifier("kisCurrentPriceRestClient")
    private val restClient: RestClient,
    private val properties: KisProperties,
) {
    private var accessToken: String? = null
    private var expiresAt: Instant = Instant.EPOCH

    @Synchronized
    fun getAccessToken(): String {
        val token = accessToken

        if (
            token != null &&
            Instant.now().isBefore(expiresAt.minusSeconds(60))
        ) {
            return token
        }

        return refresh()
    }

    @Synchronized
    fun refresh(): String {
        val response = restClient
            .post()
            .uri("/oauth2/tokenP")
            .body(
                KisTokenRequest(
                    appKey = properties.appKey,
                    appSecret = properties.appSecret,
                ),
            )
            .retrieve()
            .body(KisTokenResponse::class.java)
            ?: error("KIS token response body is empty")

        accessToken = response.accessToken
        expiresAt = Instant.now().plusSeconds(response.expiresIn)

        return response.accessToken
    }
}
