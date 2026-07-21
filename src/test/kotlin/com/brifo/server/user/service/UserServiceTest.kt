package com.brifo.server.user.service

import com.brifo.server.user.dto.request.UpdateOnboardingProfileRequest
import com.brifo.server.user.entity.OAuthProvider
import com.brifo.server.user.entity.User
import com.brifo.server.user.exception.InvalidCompanyNameException
import com.brifo.server.user.exception.InvalidNicknameException
import com.brifo.server.user.exception.OnboardingAlreadyCompletedException
import com.brifo.server.user.exception.UserNotFoundException
import com.brifo.server.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.time.LocalDateTime

class UserServiceTest {
    private lateinit var userRepository: UserRepository
    private lateinit var userService: UserService

    @BeforeEach
    fun setUp() {
        userRepository = mock(UserRepository::class.java)
        userService = UserService(userRepository)
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

    private fun user(): User =
        User.create(
            provider = OAuthProvider.KAKAO,
            socialId = "social-id",
            email = "brifo@example.com",
        )

}
