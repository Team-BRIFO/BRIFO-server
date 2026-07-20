package com.brifo.server.auth.service

import com.brifo.server.ap.entity.ApTransaction
import com.brifo.server.ap.entity.ApTransactionReason
import com.brifo.server.ap.repository.ApTransactionRepository
import com.brifo.server.auth.client.KakaoApiClient
import com.brifo.server.auth.dto.external.KakaoUserResponse
import com.brifo.server.auth.dto.request.KakaoLoginRequest
import com.brifo.server.auth.dto.response.KakaoLoginResponse
import com.brifo.server.auth.dto.response.TokenInfo
import com.brifo.server.user.entity.OAuthProvider
import com.brifo.server.user.entity.User
import com.brifo.server.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.never
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
    private lateinit var apTransactionRepository: ApTransactionRepository

    @Mock
    private lateinit var jwtTokenProvider: JwtTokenProvider

    private val clock = Clock.fixed(Instant.parse("2026-07-08T00:00:00Z"), ZoneOffset.UTC)
    private lateinit var service: KakaoLoginService

    @BeforeEach
    fun setUp() {
        service = KakaoLoginService(kakaoApiClient, userRepository, apTransactionRepository, jwtTokenProvider, clock)
    }

    @Test
    fun `기존 카카오 회원은 사용자 정보와 서비스 토큰을 받는다`() {
        val publicId = UUID.randomUUID()
        val user = User.create(OAuthProvider.KAKAO, SOCIAL_ID, "user@kakao.com", "brifo")
        ReflectionTestUtils.setField(user, "publicId", publicId)
        ReflectionTestUtils.setField(user, "onboardingCompletedAt", LocalDateTime.of(2026, 7, 7, 0, 0))
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
    fun `온보딩 미완료 카카오 회원은 새 행 없이 회원가입용 임시 토큰을 다시 받는다`() {
        val user = User.create(OAuthProvider.KAKAO, SOCIAL_ID, "user@kakao.com", "brifo")
        `when`(kakaoApiClient.getUser(AUTHORIZATION_CODE, REDIRECT_URI)).thenReturn(kakaoUser())
        `when`(userRepository.findByProviderAndSocialId(OAuthProvider.KAKAO, SOCIAL_ID)).thenReturn(user)
        `when`(jwtTokenProvider.issueSignupToken(OAuthProvider.KAKAO, SOCIAL_ID, "user@kakao.com"))
            .thenReturn("signup-token")

        val response = service.login(request()) as KakaoLoginResponse.SignupRequired

        assertEquals("signup-token", response.signupToken)
        verify(userRepository, never()).save(any(User::class.java))
        verify(apTransactionRepository, never()).save(any(ApTransaction::class.java))
    }

    @Test
    fun `신규 카카오 회원은 사용자와 초기 AP 거래를 만들고 회원가입용 임시 토큰을 받는다`() {
        `when`(kakaoApiClient.getUser(AUTHORIZATION_CODE, REDIRECT_URI)).thenReturn(kakaoUser())
        `when`(userRepository.findByProviderAndSocialId(OAuthProvider.KAKAO, SOCIAL_ID)).thenReturn(null)
        `when`(userRepository.save(any(User::class.java))).thenAnswer { it.getArgument(0) }
        `when`(jwtTokenProvider.issueSignupToken(OAuthProvider.KAKAO, SOCIAL_ID, "user@kakao.com"))
            .thenReturn("signup-token")

        val response = service.login(request()) as KakaoLoginResponse.SignupRequired

        assertEquals("signup-token", response.signupToken)
        val savedUser = org.mockito.ArgumentCaptor.forClass(User::class.java)
        verify(userRepository).save(savedUser.capture())
        assertEquals(OAuthProvider.KAKAO, savedUser.value.provider)
        assertEquals(SOCIAL_ID, savedUser.value.socialId)
        assertEquals("카카오닉네임", savedUser.value.nickname)
        assertEquals("user@kakao.com", savedUser.value.email)
        assertEquals(500, savedUser.value.balanceAp)
        assertEquals(null, savedUser.value.onboardingCompletedAt)

        val savedTransaction = org.mockito.ArgumentCaptor.forClass(ApTransaction::class.java)
        verify(apTransactionRepository).save(savedTransaction.capture())
        assertEquals(savedUser.value, savedTransaction.value.user)
        assertEquals(500, savedTransaction.value.amount)
        assertEquals(ApTransactionReason.INITIAL_GRANT, savedTransaction.value.reason)
    }

    private fun request() = KakaoLoginRequest(AUTHORIZATION_CODE, REDIRECT_URI)

    private fun kakaoUser() =
        KakaoUserResponse(
            id = SOCIAL_ID.toLong(),
            kakaoAccount =
                KakaoUserResponse.KakaoAccount(
                    email = "user@kakao.com",
                    profile = KakaoUserResponse.Profile(nickname = "카카오닉네임"),
                ),
        )

    companion object {
        private const val SOCIAL_ID = "1234567890"
        private const val AUTHORIZATION_CODE = "authorization-code"
        private const val REDIRECT_URI = "http://localhost:3000/oauth/callback/kakao"
    }
}
