package com.brifo.server.auth.service

import com.brifo.server.auth.dto.KakaoApiErrorResponse
import com.brifo.server.auth.dto.KakaoOAuthErrorResponse
import com.brifo.server.auth.dto.KakaoTokenResponse
import com.brifo.server.auth.dto.KakaoUserResponse
import com.brifo.server.global.code.ErrorCode
import com.brifo.server.global.config.KakaoProperties
import com.brifo.server.global.exception.BusinessException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import tools.jackson.databind.ObjectMapper

@Component
class KakaoApiClient(
    @Qualifier("kakaoRestClient") private val restClient: RestClient,
    private val properties: KakaoProperties,
    private val objectMapper: ObjectMapper,
) {
    fun getUser(
        authorizationCode: String,
        redirectUri: String,
    ): KakaoUserResponse {
        validateRedirectUri(redirectUri)
        val accessToken = exchangeToken(authorizationCode, redirectUri)
        return retrieveUser(accessToken)
    }

    private fun validateRedirectUri(redirectUri: String) {
        if (redirectUri !in properties.redirectUris) {
            throw BusinessException(ErrorCode.OAUTH_REDIRECT_URI_MISMATCH)
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
                .body(KakaoTokenResponse::class.java)
                ?.accessToken
                ?: throw BusinessException(ErrorCode.KAKAO_SERVER_ERROR)
        } catch (exception: HttpClientErrorException) {
            throw mapTokenError(exception)
        } catch (exception: RestClientException) {
            throw BusinessException(ErrorCode.KAKAO_SERVER_ERROR)
        }
    }

    private fun retrieveUser(accessToken: String): KakaoUserResponse {
        try {
            return restClient
                .get()
                .uri(KAKAO_USER_URI)
                .headers { it.setBearerAuth(accessToken) }
                .retrieve()
                .body(KakaoUserResponse::class.java)
                ?: throw BusinessException(ErrorCode.KAKAO_SERVER_ERROR)
        } catch (exception: HttpClientErrorException) {
            val error = parseError(exception.responseBodyAsString, KakaoApiErrorResponse::class.java)
            if (exception.statusCode.value() == 401 || error?.code == KAKAO_INVALID_TOKEN_CODE) {
                throw BusinessException(ErrorCode.OAUTH_INVALID_TOKEN)
            }
            throw BusinessException(ErrorCode.KAKAO_SERVER_ERROR)
        } catch (exception: RestClientException) {
            throw BusinessException(ErrorCode.KAKAO_SERVER_ERROR)
        }
    }

    private fun mapTokenError(exception: HttpClientErrorException): BusinessException {
        val error = parseError(exception.responseBodyAsString, KakaoOAuthErrorResponse::class.java)
        val isRedirectMismatch =
            error?.error == "invalid_grant" &&
                error.errorDescription?.contains("redirect", ignoreCase = true) == true

        val errorCode =
            when {
                isRedirectMismatch -> ErrorCode.OAUTH_REDIRECT_URI_MISMATCH
                error?.error == "invalid_grant" -> ErrorCode.OAUTH_INVALID_AUTHORIZATION_CODE
                else -> ErrorCode.KAKAO_SERVER_ERROR
            }
        return BusinessException(errorCode)
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
