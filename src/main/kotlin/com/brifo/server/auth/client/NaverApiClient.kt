package com.brifo.server.auth.client

import com.brifo.server.auth.dto.external.NaverTokenResponse
import com.brifo.server.auth.dto.external.NaverUserResponse
import com.brifo.server.auth.dto.internal.OAuthUserProfile
import com.brifo.server.auth.exception.AuthException
import com.brifo.server.auth.exception.InvalidAuthorizationCodeException
import com.brifo.server.auth.exception.InvalidOAuthTokenException
import com.brifo.server.auth.exception.OAuthProviderServerException
import com.brifo.server.auth.exception.OAuthRedirectUriMismatchException
import com.brifo.server.global.config.NaverProperties
import com.brifo.server.user.entity.OAuthProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.body
import tools.jackson.databind.ObjectMapper

@Component
class NaverApiClient(
    @Qualifier("naverRestClient") private val restClient: RestClient,
    private val properties: NaverProperties,
    private val objectMapper: ObjectMapper,
) {
    fun authenticate(
        authorizationCode: String,
        state: String,
        redirectUri: String,
    ): OAuthUserProfile {
        validateRedirectUri(redirectUri)
        val accessToken = exchangeToken(authorizationCode, state)
        val response = retrieveUser(accessToken)
        return OAuthUserProfile(
            provider = OAuthProvider.NAVER,
            socialId = response.id,
            email = response.email,
            nickname = response.nickname,
        )
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
                    .body<NaverTokenResponse>()
                    ?: throw OAuthProviderServerException()

            response.error?.let { throw mapTokenError(it) }
            return response.accessToken ?: throw OAuthProviderServerException()
        } catch (exception: HttpClientErrorException) {
            val response = parseError(exception.responseBodyAsString)
            response?.error?.let { throw mapTokenError(it) }
            throw OAuthProviderServerException()
        } catch (exception: RestClientException) {
            throw OAuthProviderServerException()
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
                    .body<NaverUserResponse>()
                    ?: throw OAuthProviderServerException()

            if (response.resultcode in INVALID_TOKEN_RESULT_CODES) {
                throw InvalidOAuthTokenException()
            }
            if (response.resultcode != SUCCESS_RESULT_CODE || response.response == null) {
                throw OAuthProviderServerException()
            }
            return response.response
        } catch (_: HttpClientErrorException.Unauthorized) {
            throw InvalidOAuthTokenException()
        } catch (_: RestClientException) {
            throw OAuthProviderServerException()
        }
    }

    private fun parseError(body: String): NaverTokenResponse? =
        runCatching { objectMapper.readValue(body, NaverTokenResponse::class.java) }.getOrNull()

    private fun mapTokenError(error: String): AuthException =
        if (error in INVALID_AUTHORIZATION_ERRORS) {
            InvalidAuthorizationCodeException()
        } else {
            OAuthProviderServerException()
        }

    companion object {
        private const val NAVER_TOKEN_URI = "https://nid.naver.com/oauth2.0/token"
        private const val NAVER_USER_URI = "https://openapi.naver.com/v1/nid/me"
        private const val SUCCESS_RESULT_CODE = "00"
        private val INVALID_TOKEN_RESULT_CODES = setOf("024", "028")
        private val INVALID_AUTHORIZATION_ERRORS = setOf("invalid_request", "invalid_grant")
    }
}
