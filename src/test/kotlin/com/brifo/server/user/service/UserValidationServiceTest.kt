package com.brifo.server.user.service

import com.brifo.server.stock.exception.DuplicatedStockSelectionException
import com.brifo.server.stock.exception.StockSelectionMaximumExceededException
import com.brifo.server.stock.exception.StockSelectionMinimumNotMetException
import com.brifo.server.user.entity.OAuthProvider
import com.brifo.server.user.entity.User
import com.brifo.server.user.exception.InvalidCompanyNameException
import com.brifo.server.user.exception.InvalidNicknameException
import com.brifo.server.user.exception.OnboardingAlreadyCompletedException
import com.brifo.server.user.exception.OnboardingProfileNotCompletedException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class UserValidationServiceTest {
    private val validationService = UserValidationService()

    @Test
    fun `프로필 문자열은 공백을 제거하고 회사명 null은 유지한다`() {
        assertEquals("닉네임", validationService.normalizeNickname("  닉네임  "))
        assertEquals("회사", validationService.normalizeCompanyName("  회사  "))
        assertNull(validationService.normalizeCompanyName(null))
    }

    @Test
    fun `프로필 문자열 길이 범위를 검증한다`() {
        listOf("", " ", "a".repeat(51)).forEach {
            assertThrows(InvalidNicknameException::class.java) {
                validationService.normalizeNickname(it)
            }
        }
        listOf("", " ", "a".repeat(101)).forEach {
            assertThrows(InvalidCompanyNameException::class.java) {
                validationService.normalizeCompanyName(it)
            }
        }
    }

    @Test
    fun `관심 종목은 한 개에서 세 개까지 중복 없이 선택한다`() {
        validationService.validateStockIds(listOf(UUID.randomUUID()))
        validationService.validateStockIds(List(3) { UUID.randomUUID() })

        assertThrows(StockSelectionMinimumNotMetException::class.java) {
            validationService.validateStockIds(emptyList())
        }
        assertThrows(StockSelectionMaximumExceededException::class.java) {
            validationService.validateStockIds(List(4) { UUID.randomUUID() })
        }
        val duplicatedId = UUID.randomUUID()
        assertThrows(DuplicatedStockSelectionException::class.java) {
            validationService.validateStockIds(listOf(duplicatedId, duplicatedId))
        }
    }

    @Test
    fun `온보딩 상태에 맞는 요청만 허용한다`() {
        val pendingUser = user()
        val completedUser = user().also { it.completeOnboarding(LocalDateTime.now()) }

        validationService.requireOnboardingPending(pendingUser)
        validationService.requireOnboardingCompleted(completedUser)
        assertThrows(OnboardingAlreadyCompletedException::class.java) {
            validationService.requireOnboardingPending(completedUser)
        }
        assertThrows(OnboardingProfileNotCompletedException::class.java) {
            validationService.requireOnboardingCompleted(pendingUser)
        }
    }

    private fun user(): User =
        User.create(OAuthProvider.KAKAO, "social-id", "user@example.com")
}
