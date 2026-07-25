package com.brifo.server.auth.service

import com.brifo.server.auth.client.KakaoApiClient
import com.brifo.server.auth.dto.request.KakaoLoginRequest
import com.brifo.server.auth.dto.response.OAuthLoginResponse
import org.springframework.stereotype.Service

@Service
class KakaoLoginService(
    private val kakaoApiClient: KakaoApiClient,
    private val oauthLoginService: OAuthLoginService,
) {
    fun login(request: KakaoLoginRequest): OAuthLoginResponse {
        val profile =
            kakaoApiClient.authenticate(
                authorizationCode = request.authorizationCode,
                redirectUri = request.redirectUri,
            )
        return oauthLoginService.login(profile)
    }
}
