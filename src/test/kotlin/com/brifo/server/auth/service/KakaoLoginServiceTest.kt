package com.brifo.server.auth.service

import com.brifo.server.auth.client.KakaoApiClient
import com.brifo.server.auth.dto.internal.OAuthUserProfile
import com.brifo.server.auth.dto.request.KakaoLoginRequest
import com.brifo.server.auth.dto.response.OAuthLoginResponse
import com.brifo.server.user.entity.OAuthProvider
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class KakaoLoginServiceTest {
    @Mock
    private lateinit var kakaoApiClient: KakaoApiClient

    @Mock
    private lateinit var oauthLoginService: OAuthLoginService

    private lateinit var service: KakaoLoginService

    @BeforeEach
    fun setUp() {
        service = KakaoLoginService(kakaoApiClient, oauthLoginService)
    }

    @Test
    fun `카카오 인증 결과를 공통 로그인 서비스에 전달한다`() {
        val profile = OAuthUserProfile(OAuthProvider.KAKAO, SOCIAL_ID, "user@kakao.com", "brifo")
        val expected = OAuthLoginResponse.SignupRequired(signupToken = "signup-token")
        `when`(kakaoApiClient.authenticate(AUTHORIZATION_CODE, REDIRECT_URI)).thenReturn(profile)
        `when`(oauthLoginService.login(profile)).thenReturn(expected)

        val response = service.login(KakaoLoginRequest(AUTHORIZATION_CODE, REDIRECT_URI))

        assertSame(expected, response)
        verify(oauthLoginService).login(profile)
    }

    companion object {
        private const val SOCIAL_ID = "1234567890"
        private const val AUTHORIZATION_CODE = "authorization-code"
        private const val REDIRECT_URI = "http://localhost:3000/oauth/callback/kakao"
    }
}
