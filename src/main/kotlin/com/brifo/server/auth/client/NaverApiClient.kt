package com.brifo.server.auth.client

import com.brifo.server.auth.dto.external.NaverTokenResponse
import com.brifo.server.auth.dto.external.NaverUserResponse
import com.brifo.server.auth.exception.AuthException
import com.brifo.server.auth.exception.InvalidAuthorizationCodeException
import com.brifo.server.auth.exception.InvalidTokenException
import com.brifo.server.auth.exception.NaverServerException
import com.brifo.server.auth.exception.OAuthRedirectUriMismatchException
import com.brifo.server.global.config.NaverProperties
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import tools.jackson.databind.ObjectMapper

@Component
class NaverApiClient(
    @Qualifier("naverRestClient") private val restClient: RestClient,
    private val properties: NaverProperties,
    private val objectMapper: ObjectMapper,
) {
    fun getUser(
        authorizationCode: String,
        state: String,
        redirectUri: String,
    ): NaverUserResponse.Profile {
        validateRedirectUri(redirectUri)
        val accessToken = exchangeToken(authorizationCode, state)
        return retrieveUser(accessToken)
    }

    private fun validateRedirectUri(redirectUri: String) {
        if (redirectUri !in properties.redirectUris) {
            throw OAuthRedirectUriMismatchException()
        }
    }

    private fun exchangeToken(
        authorizationCode: String,
        state: String,
    ): String {
        val form =
            LinkedMultiValueMap<String, String>().apply {
                add("grant_type", "authorization_code")
                add("client_id", properties.clientId)
                add("client_secret", properties.clientSecret)
                add("code", authorizationCode)
                add("state", state)
            }

        try {
            val response =
                restClient
                    .post()
                    .uri(NAVER_TOKEN_URI)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(NaverTokenResponse::class.java)
                    ?: throw NaverServerException()

            response.error?.let { throw mapTokenError(it) }
            return response.accessToken ?: throw NaverServerException()
        } catch (exception: HttpClientErrorException) {
            val response = parseError(exception.responseBodyAsString)
            response?.error?.let { throw mapTokenError(it) }
            throw NaverServerException()
        } catch (exception: RestClientException) {
            throw NaverServerException()
        }
    }

    private fun retrieveUser(accessToken: String): NaverUserResponse.Profile {
        try {
            val response =
                restClient
                    .get()
                    .uri(NAVER_USER_URI)
                    .headers { it.setBearerAuth(accessToken) }
                    .retrieve()
                    .body(NaverUserResponse::class.java)
                    ?: throw NaverServerException()

            if (response.resultcode in INVALID_TOKEN_RESULT_CODES) {
                throw InvalidTokenException()
            }
            if (response.resultcode != SUCCESS_RESULT_CODE || response.response == null) {
                throw NaverServerException()
            }
            return response.response
        } catch (exception: HttpClientErrorException.Unauthorized) {
            throw InvalidTokenException()
        } catch (exception: RestClientException) {
            throw NaverServerException()
        }
    }

    private fun parseError(body: String): NaverTokenResponse? =
        runCatching { objectMapper.readValue(body, NaverTokenResponse::class.java) }.getOrNull()

    private fun mapTokenError(error: String): AuthException =
        if (error in INVALID_AUTHORIZATION_ERRORS) {
            InvalidAuthorizationCodeException()
        } else {
            NaverServerException()
        }

    companion object {
        private const val NAVER_TOKEN_URI = "https://nid.naver.com/oauth2.0/token"
        private const val NAVER_USER_URI = "https://openapi.naver.com/v1/nid/me"
        private const val SUCCESS_RESULT_CODE = "00"
        private val INVALID_TOKEN_RESULT_CODES = setOf("024", "028")
        private val INVALID_AUTHORIZATION_ERRORS = setOf("invalid_request", "invalid_grant")
    }
}
