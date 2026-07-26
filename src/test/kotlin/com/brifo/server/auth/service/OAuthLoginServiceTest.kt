package com.brifo.server.auth.service

import com.brifo.server.ap.entity.ApTransaction
import com.brifo.server.ap.entity.ApTransactionReason
import com.brifo.server.ap.repository.ApTransactionRepository
import com.brifo.server.auth.dto.internal.OAuthUserProfile
import com.brifo.server.auth.dto.response.OAuthLoginResponse
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
class OAuthLoginServiceTest {
    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var apTransactionRepository: ApTransactionRepository

    @Mock
    private lateinit var jwtTokenProvider: JwtTokenProvider

    private val clock = Clock.fixed(Instant.parse("2026-07-08T00:00:00Z"), ZoneOffset.UTC)
    private lateinit var service: OAuthLoginService

    @BeforeEach
    fun setUp() {
        service = OAuthLoginService(userRepository, apTransactionRepository, jwtTokenProvider, clock)
    }

    @Test
    fun `기존 회원은 사용자 정보와 서비스 토큰을 받는다`() {
        val publicId = UUID.randomUUID()
        val user = User.create(OAuthProvider.KAKAO, SOCIAL_ID, EMAIL, "brifo")
        ReflectionTestUtils.setField(user, "publicId", publicId)
        ReflectionTestUtils.setField(user, "onboardingCompletedAt", LocalDateTime.of(2026, 7, 7, 0, 0))
        val tokenInfo = TokenInfo("access-token", "refresh-token", 3600, 604800)
        `when`(userRepository.findByProviderAndSocialId(OAuthProvider.KAKAO, SOCIAL_ID)).thenReturn(user)
        `when`(jwtTokenProvider.issueLoginTokens(publicId)).thenReturn(tokenInfo)

        val response = service.login(profile()) as OAuthLoginResponse.Login

        assertEquals(publicId, response.user.userId)
        assertEquals("brifo", response.user.nickname)
        assertEquals(tokenInfo, response.token)
        assertEquals(LocalDateTime.of(2026, 7, 8, 0, 0), user.lastLoginAt)
    }

    @Test
    fun `온보딩 미완료 회원은 새 행 없이 회원가입용 임시 토큰을 다시 받는다`() {
        val user = User.create(OAuthProvider.KAKAO, SOCIAL_ID, EMAIL, "brifo")
        ReflectionTestUtils.setField(user, "publicId", USER_ID)
        `when`(userRepository.findByProviderAndSocialId(OAuthProvider.KAKAO, SOCIAL_ID)).thenReturn(user)
        `when`(jwtTokenProvider.issueSignupToken(USER_ID)).thenReturn("signup-token")

        val response = service.login(profile()) as OAuthLoginResponse.SignupRequired

        assertEquals("signup-token", response.signupToken)
        verify(userRepository, never()).saveAndFlush(any(User::class.java))
        verify(apTransactionRepository, never()).save(any(ApTransaction::class.java))
    }

    @Test
    fun `신규 회원은 사용자와 초기 AP 거래를 만들고 회원가입용 임시 토큰을 받는다`() {
        `when`(userRepository.findByProviderAndSocialId(OAuthProvider.KAKAO, SOCIAL_ID)).thenReturn(null)
        `when`(userRepository.saveAndFlush(any(User::class.java))).thenAnswer {
            it.getArgument<User>(0).also { user -> ReflectionTestUtils.setField(user, "publicId", USER_ID) }
        }
        `when`(jwtTokenProvider.issueSignupToken(USER_ID)).thenReturn("signup-token")

        val response = service.login(profile()) as OAuthLoginResponse.SignupRequired

        assertEquals("signup-token", response.signupToken)
        val savedUser = org.mockito.ArgumentCaptor.forClass(User::class.java)
        verify(userRepository).saveAndFlush(savedUser.capture())
        assertEquals(OAuthProvider.KAKAO, savedUser.value.provider)
        assertEquals(SOCIAL_ID, savedUser.value.socialId)
        assertEquals("brifo", savedUser.value.nickname)
        assertEquals(EMAIL, savedUser.value.email)
        assertEquals(500, savedUser.value.balanceAp)

        val savedTransaction = org.mockito.ArgumentCaptor.forClass(ApTransaction::class.java)
        verify(apTransactionRepository).save(savedTransaction.capture())
        assertEquals(savedUser.value, savedTransaction.value.user)
        assertEquals(500, savedTransaction.value.amount)
        assertEquals(ApTransactionReason.INITIAL_GRANT, savedTransaction.value.reason)
    }

    @Test
    fun `제공자 닉네임이 없으면 신규 회원의 닉네임은 null로 저장한다`() {
        `when`(userRepository.findByProviderAndSocialId(OAuthProvider.KAKAO, SOCIAL_ID)).thenReturn(null)
        `when`(userRepository.saveAndFlush(any(User::class.java))).thenAnswer {
            it.getArgument<User>(0).also { user -> ReflectionTestUtils.setField(user, "publicId", USER_ID) }
        }
        `when`(jwtTokenProvider.issueSignupToken(USER_ID)).thenReturn("signup-token")

        service.login(OAuthUserProfile(OAuthProvider.KAKAO, SOCIAL_ID, EMAIL, null))

        val savedUser = org.mockito.ArgumentCaptor.forClass(User::class.java)
        verify(userRepository).saveAndFlush(savedUser.capture())
        assertEquals(null, savedUser.value.nickname)
    }

    private fun profile() = OAuthUserProfile(OAuthProvider.KAKAO, SOCIAL_ID, EMAIL, "brifo")

    companion object {
        private const val SOCIAL_ID = "1234567890"
        private const val EMAIL = "user@kakao.com"
        private val USER_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
    }
}
