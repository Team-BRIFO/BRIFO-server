package com.brifo.server.user.service

import com.brifo.server.user.dto.request.UpdateOnboardingProfileRequest
import com.brifo.server.user.entity.OAuthProvider
import com.brifo.server.user.entity.User
import com.brifo.server.user.exception.InvalidCompanyNameException
import com.brifo.server.user.exception.InvalidNicknameException
import com.brifo.server.user.exception.OnboardingAlreadyCompletedException
import com.brifo.server.user.exception.UserNotFoundException
import com.brifo.server.user.repository.UserMyPageQueryRepository
import com.brifo.server.user.repository.UserMyPageStats
import com.brifo.server.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

class UserServiceTest {
    private lateinit var userRepository: UserRepository
    private lateinit var userMyPageQueryRepository: UserMyPageQueryRepository
    private lateinit var userService: UserService

    private val clock = Clock.fixed(Instant.parse("2026-07-21T09:00:00Z"), ZoneId.of("Asia/Seoul"))

    @BeforeEach
    fun setUp() {
        userRepository = mock(UserRepository::class.java)
        userMyPageQueryRepository = mock(UserMyPageQueryRepository::class.java)
        userService = UserService(userRepository, userMyPageQueryRepository, clock)
    }

    @Test
    fun `온보딩 프로필은 토큰 사용자의 소셜 식별자로 조회하고 trim 후 저장한다`() {
        val user = user()
        `when`(userRepository.findByProviderAndSocialId(OAuthProvider.KAKAO, "social-id")).thenReturn(user)

        userService.updateOnboardingProfile(
            provider = OAuthProvider.KAKAO,
            socialId = "social-id",
            request =
                UpdateOnboardingProfileRequest(
                    nickname = "  brifo  ",
                    companyName = "  내 투자회사  ",
                ),
        )

        verify(userRepository).findByProviderAndSocialId(OAuthProvider.KAKAO, "social-id")
        assertEquals("brifo", user.nickname)
        assertEquals("내 투자회사", user.companyName)
    }

    @Test
    fun `회사명이 없으면 기존 기본 회사명을 유지한다`() {
        val user = user()
        `when`(userRepository.findByProviderAndSocialId(OAuthProvider.KAKAO, "social-id")).thenReturn(user)

        userService.updateOnboardingProfile(
            provider = OAuthProvider.KAKAO,
            socialId = "social-id",
            request = UpdateOnboardingProfileRequest(nickname = "brifo"),
        )

        assertEquals("brifo", user.nickname)
        assertEquals("내 투자회사", user.companyName)
    }

    @Test
    fun `닉네임은 trim 후 1자에서 50자까지 허용한다`() {
        listOf("   ", "a".repeat(51)).forEach { nickname ->
            assertThrows(InvalidNicknameException::class.java) {
                userService.updateOnboardingProfile(
                    provider = OAuthProvider.KAKAO,
                    socialId = "social-id",
                    request = UpdateOnboardingProfileRequest(nickname = nickname),
                )
            }
        }

        verifyNoInteractions(userRepository)
    }

    @Test
    fun `입력된 회사명은 trim 후 1자에서 100자까지 허용한다`() {
        listOf("   ", "a".repeat(101)).forEach { companyName ->
            assertThrows(InvalidCompanyNameException::class.java) {
                userService.updateOnboardingProfile(
                    provider = OAuthProvider.KAKAO,
                    socialId = "social-id",
                    request =
                        UpdateOnboardingProfileRequest(
                            nickname = "brifo",
                            companyName = companyName,
                        ),
                )
            }
        }

        verifyNoInteractions(userRepository)
    }

    @Test
    fun `존재하지 않는 소셜 사용자는 USER_404 예외를 던진다`() {
        `when`(userRepository.findByProviderAndSocialId(OAuthProvider.NAVER, "unknown-id")).thenReturn(null)

        assertThrows(UserNotFoundException::class.java) {
            userService.updateOnboardingProfile(
                provider = OAuthProvider.NAVER,
                socialId = "unknown-id",
                request = UpdateOnboardingProfileRequest(nickname = "brifo"),
            )
        }
    }

    @Test
    fun `온보딩을 완료한 사용자는 프로필을 다시 저장할 수 없다`() {
        val user = mock(User::class.java)
        `when`(user.onboardingCompletedAt).thenReturn(LocalDateTime.of(2026, 7, 21, 16, 0))
        `when`(userRepository.findByProviderAndSocialId(OAuthProvider.KAKAO, "social-id")).thenReturn(user)

        assertThrows(OnboardingAlreadyCompletedException::class.java) {
            userService.updateOnboardingProfile(
                provider = OAuthProvider.KAKAO,
                socialId = "social-id",
                request = UpdateOnboardingProfileRequest(nickname = "brifo"),
            )
        }
    }

    @Test
    fun `마이페이지는 사용자 정보와 집계 통계를 반환한다`() {
        val userPublicId = UUID.randomUUID()
        val user = myPageUser()
        val weekStart = LocalDateTime.of(2026, 7, 20, 0, 0)
        val now = LocalDateTime.of(2026, 7, 21, 18, 0)
        val stats =
            UserMyPageStats(
                thisWeekEarnedAp = 450,
                correctDecisionCount = 2,
                settledDecisionCount = 3,
                totalDecisionCount = 48,
                latestConsecutiveDays = 5,
                latestAttendanceAt = LocalDateTime.of(2026, 7, 20, 9, 0),
                learnedTermCount = 24,
            )
        `when`(userRepository.findByPublicId(userPublicId)).thenReturn(user)
        `when`(userMyPageQueryRepository.getMyPageStats(7L, weekStart, now)).thenReturn(stats)

        val response = userService.getMyPage(userPublicId)

        assertEquals("brifo", response.nickname)
        assertEquals("내 투자회사", response.companyName)
        assertEquals(1250, response.balanceAp)
        assertEquals(450, response.thisWeekEarnedAp)
        assertEquals(67, response.decisionAccuracyRate)
        assertEquals(48, response.totalDecision)
        assertEquals(5, response.consecutiveDays)
        assertEquals(24, response.learnedTermCount)
        verify(userMyPageQueryRepository).getMyPageStats(7L, weekStart, now)
    }

    @Test
    fun `정산된 결정과 유효한 최근 출석이 없으면 적중률과 연속 출석은 0이다`() {
        val userPublicId = UUID.randomUUID()
        val user = myPageUser()
        val stats =
            UserMyPageStats(
                thisWeekEarnedAp = 0,
                correctDecisionCount = 0,
                settledDecisionCount = 0,
                totalDecisionCount = 2,
                latestConsecutiveDays = 7,
                latestAttendanceAt = LocalDateTime.of(2026, 7, 19, 9, 0),
                learnedTermCount = 0,
            )
        `when`(userRepository.findByPublicId(userPublicId)).thenReturn(user)
        `when`(
            userMyPageQueryRepository.getMyPageStats(
                7L,
                LocalDateTime.of(2026, 7, 20, 0, 0),
                LocalDateTime.of(2026, 7, 21, 18, 0),
            ),
        ).thenReturn(stats)

        val response = userService.getMyPage(userPublicId)

        assertEquals(0, response.decisionAccuracyRate)
        assertEquals(0, response.consecutiveDays)
    }

    @Test
    fun `마이페이지 사용자가 없으면 USER_404 예외를 던진다`() {
        val userPublicId = UUID.randomUUID()
        `when`(userRepository.findByPublicId(userPublicId)).thenReturn(null)

        assertThrows(UserNotFoundException::class.java) {
            userService.getMyPage(userPublicId)
        }

        verifyNoInteractions(userMyPageQueryRepository)
    }

    private fun user(): User =
        User.create(
            provider = OAuthProvider.KAKAO,
            socialId = "social-id",
            email = "brifo@example.com",
        )

    private fun myPageUser(): User =
        mock(User::class.java).also {
            `when`(it.id).thenReturn(7L)
            `when`(it.nickname).thenReturn("brifo")
            `when`(it.companyName).thenReturn("내 투자회사")
            `when`(it.balanceAp).thenReturn(1250)
        }
}
