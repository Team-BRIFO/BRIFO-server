package com.brifo.server.auth.service

import com.brifo.server.auth.dto.KakaoInfo
import com.brifo.server.auth.dto.KakaoLoginRequest
import com.brifo.server.auth.dto.KakaoLoginResponse
import com.brifo.server.auth.dto.KakaoUserResponse
import com.brifo.server.auth.dto.TokenInfo
import com.brifo.server.user.entity.OAuthProvider
import com.brifo.server.user.entity.User
import com.brifo.server.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class KakaoLoginServiceTest {
    @Mock
    private lateinit var kakaoApiClient: KakaoApiClient

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var jwtTokenProvider: JwtTokenProvider

    private val clock = Clock.fixed(Instant.parse("2026-07-08T00:00:00Z"), ZoneOffset.UTC)
    private lateinit var service: KakaoLoginService

    @BeforeEach
    fun setUp() {
        service = KakaoLoginService(kakaoApiClient, userRepository, jwtTokenProvider, clock)
    }

    @Test
    fun `기존 카카오 회원은 사용자 정보와 서비스 토큰을 받는다`() {
        val publicId = UUID.randomUUID()
        val user = User.create(OAuthProvider.KAKAO, SOCIAL_ID, "brifo", "user@kakao.com")
        ReflectionTestUtils.setField(user, "publicId", publicId)
        val tokenInfo = TokenInfo("access-token", "refresh-token", 3600, 604800)
        `when`(kakaoApiClient.getUser(AUTHORIZATION_CODE, REDIRECT_URI)).thenReturn(kakaoUser())
        `when`(userRepository.findByProviderAndSocialId(OAuthProvider.KAKAO, SOCIAL_ID)).thenReturn(user)
        `when`(jwtTokenProvider.issueLoginTokens(publicId)).thenReturn(tokenInfo)

        val response = service.login(request()) as KakaoLoginResponse.Login

        assertEquals(publicId, response.user.userId)
        assertEquals("brifo", response.user.nickname)
        assertEquals(tokenInfo, response.token)
        assertEquals(LocalDateTime.of(2026, 7, 8, 0, 0), user.lastLoginAt)
        verify(jwtTokenProvider).issueLoginTokens(publicId)
    }

    @Test
    fun `신규 카카오 회원은 회원가입용 임시 토큰을 받는다`() {
        `when`(kakaoApiClient.getUser(AUTHORIZATION_CODE, REDIRECT_URI)).thenReturn(kakaoUser())
        `when`(userRepository.findByProviderAndSocialId(OAuthProvider.KAKAO, SOCIAL_ID)).thenReturn(null)
        `when`(jwtTokenProvider.issueSignupToken(KakaoInfo(SOCIAL_ID, "user@kakao.com")))
            .thenReturn("signup-token")

        val response = service.login(request()) as KakaoLoginResponse.SignupRequired

        assertEquals("signup-token", response.signupToken)
        assertEquals(SOCIAL_ID, response.kakaoInfo.id)
        assertEquals("user@kakao.com", response.kakaoInfo.email)
    }

    private fun request() = KakaoLoginRequest(AUTHORIZATION_CODE, REDIRECT_URI)

    private fun kakaoUser() =
        KakaoUserResponse(
            id = SOCIAL_ID.toLong(),
            kakaoAccount = KakaoUserResponse.KakaoAccount(email = "user@kakao.com"),
        )

    companion object {
        private const val SOCIAL_ID = "1234567890"
        private const val AUTHORIZATION_CODE = "authorization-code"
        private const val REDIRECT_URI = "http://localhost:3000/oauth/callback/kakao"
    }
}
