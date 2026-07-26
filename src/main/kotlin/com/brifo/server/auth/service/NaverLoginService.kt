package com.brifo.server.auth.service

import com.brifo.server.auth.client.NaverApiClient
import com.brifo.server.auth.dto.request.NaverLoginRequest
import com.brifo.server.auth.dto.response.OAuthLoginResponse
import org.springframework.stereotype.Service

@Service
class NaverLoginService(
    private val naverApiClient: NaverApiClient,
    private val oauthLoginService: OAuthLoginService,
) {
    fun login(request: NaverLoginRequest): OAuthLoginResponse {
        val profile =
            naverApiClient.authenticate(
                authorizationCode = request.authorizationCode,
                state = request.state,
                redirectUri = request.redirectUri,
            )
        return oauthLoginService.login(profile)
    }
}
