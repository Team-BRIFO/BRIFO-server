package com.brifo.server.auth.client

import com.brifo.server.auth.dto.external.KakaoApiErrorResponse
import com.brifo.server.auth.dto.external.KakaoOAuthErrorResponse
import com.brifo.server.auth.dto.external.KakaoTokenResponse
import com.brifo.server.auth.dto.external.KakaoUserResponse
import com.brifo.server.auth.dto.internal.OAuthUserProfile
import com.brifo.server.auth.exception.AuthException
import com.brifo.server.auth.exception.InvalidAuthorizationCodeException
import com.brifo.server.auth.exception.InvalidOAuthTokenException
import com.brifo.server.auth.exception.OAuthProviderServerException
import com.brifo.server.auth.exception.OAuthRedirectUriMismatchException
import com.brifo.server.global.config.KakaoProperties
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
class KakaoApiClient(
    @Qualifier("kakaoRestClient") private val restClient: RestClient,
    private val properties: KakaoProperties,
    private val objectMapper: ObjectMapper,
) {
    fun authenticate(
        authorizationCode: String,
        redirectUri: String,
    ): OAuthUserProfile {
        validateRedirectUri(redirectUri)
        val accessToken = exchangeToken(authorizationCode, redirectUri)
        val response = retrieveUser(accessToken)
        return OAuthUserProfile(
            provider = OAuthProvider.KAKAO,
            socialId = response.id.toString(),
            email = response.kakaoAccount?.email,
            nickname = response.kakaoAccount?.profile?.nickname,
        )
    }

    private fun validateRedirectUri(redirectUri: String) {
        if (redirectUri !in properties.redirectUris) {
            throw OAuthRedirectUriMismatchException()
        }
    }

    private fun exchangeToken(
        authorizationCode: String,
        redirectUri: String,
    ): String {
        val form =
            LinkedMultiValueMap<String, String>().apply {
                add("grant_type", "authorization_code")
                add("client_id", properties.clientId)
                add("redirect_uri", redirectUri)
                add("code", authorizationCode)
                properties.clientSecret
                    ?.takeIf { it.isNotBlank() }
                    ?.let { add("client_secret", it) }
            }

        try {
            return restClient
                .post()
                .uri(KAKAO_TOKEN_URI)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body<KakaoTokenResponse>()
                ?.accessToken
                ?: throw OAuthProviderServerException()
        } catch (exception: HttpClientErrorException) {
            throw mapTokenError(exception)
        } catch (_: RestClientException) {
            throw OAuthProviderServerException()
        }
    }

    private fun retrieveUser(accessToken: String): KakaoUserResponse {
        try {
            return restClient
                .get()
                .uri(KAKAO_USER_URI)
                .headers { it.setBearerAuth(accessToken) }
                .retrieve()
                .body<KakaoUserResponse>()
                ?: throw OAuthProviderServerException()
        } catch (exception: HttpClientErrorException) {
            val error = parseError(exception.responseBodyAsString, KakaoApiErrorResponse::class.java)
            if (exception.statusCode.value() == 401 || error?.code == KAKAO_INVALID_TOKEN_CODE) {
                throw InvalidOAuthTokenException()
            }
            throw OAuthProviderServerException()
        } catch (_: RestClientException) {
            throw OAuthProviderServerException()
        }
    }

    private fun mapTokenError(exception: HttpClientErrorException): AuthException {
        val error = parseError(exception.responseBodyAsString, KakaoOAuthErrorResponse::class.java)
        val isRedirectMismatch =
            error?.error == "invalid_grant" &&
                error.errorDescription?.contains("redirect", ignoreCase = true) == true

        return when {
            isRedirectMismatch -> OAuthRedirectUriMismatchException()
            error?.error == "invalid_grant" -> InvalidAuthorizationCodeException()
            else -> OAuthProviderServerException()
        }
    }

    private fun <T> parseError(
        body: String,
        type: Class<T>,
    ): T? = runCatching { objectMapper.readValue(body, type) }.getOrNull()

    companion object {
        private const val KAKAO_TOKEN_URI = "https://kauth.kakao.com/oauth/token"
        private const val KAKAO_USER_URI = "https://kapi.kakao.com/v2/user/me"
        private const val KAKAO_INVALID_TOKEN_CODE = -401
    }
}
