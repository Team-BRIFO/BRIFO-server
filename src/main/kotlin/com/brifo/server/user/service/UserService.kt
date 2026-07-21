package com.brifo.server.user.service

import com.brifo.server.user.dto.request.UpdateOnboardingProfileRequest
import com.brifo.server.user.dto.response.GetMyPageResponse
import com.brifo.server.user.entity.OAuthProvider
import com.brifo.server.user.exception.InvalidCompanyNameException
import com.brifo.server.user.exception.InvalidNicknameException
import com.brifo.server.user.exception.OnboardingAlreadyCompletedException
import com.brifo.server.user.exception.UserNotFoundException
import com.brifo.server.user.repository.UserMyPageQueryRepository
import com.brifo.server.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import java.util.UUID
import kotlin.math.roundToInt

@Service
class UserService(
    private val userRepository: UserRepository,
    private val userMyPageQueryRepository: UserMyPageQueryRepository,
    private val clock: Clock,
) {
    @Transactional
    fun updateOnboardingProfile(
        provider: OAuthProvider,
        socialId: String,
        request: UpdateOnboardingProfileRequest,
    ) {
        val nickname = validateNickname(request.nickname)
        val companyName = validateCompanyName(request.companyName)
        val user = userRepository.findByProviderAndSocialId(provider, socialId) ?: throw UserNotFoundException()

        if (user.onboardingCompletedAt != null) {
            throw OnboardingAlreadyCompletedException()
        }

        user.updateOnboardingProfile(nickname, companyName)
    }

    @Transactional(readOnly = true)
    fun getMyPage(userPublicId: UUID): GetMyPageResponse {
        val user = userRepository.findByPublicId(userPublicId) ?: throw UserNotFoundException()
        val userId = requireNotNull(user.id) { "Persisted user must have an id." }
        val now = LocalDateTime.now(clock)
        val today = LocalDate.now(clock)
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay()
        val stats = userMyPageQueryRepository.getMyPageStats(userId, weekStart, now)

        return GetMyPageResponse(
            nickname = requireNotNull(user.nickname) { "Onboarded user must have a nickname." },
            companyName = user.companyName,
            balanceAp = user.balanceAp,
            thisWeekEarnedAp = stats.thisWeekEarnedAp,
            decisionAccuracyRate = calculateAccuracyRate(stats.correctDecisionCount, stats.settledDecisionCount),
            totalDecision = Math.toIntExact(stats.totalDecisionCount),
            consecutiveDays = calculateConsecutiveDays(stats.latestConsecutiveDays, stats.latestAttendanceAt, today),
            learnedTermCount = Math.toIntExact(stats.learnedTermCount),
        )
    }

    private fun validateNickname(nickname: String): String =
        nickname.trim().takeIf { it.length in NICKNAME_LENGTH_RANGE }
            ?: throw InvalidNicknameException()

    private fun validateCompanyName(companyName: String?): String? {
        if (companyName == null) return null

        return companyName.trim().takeIf { it.length in COMPANY_NAME_LENGTH_RANGE }
            ?: throw InvalidCompanyNameException()
    }

    private fun calculateAccuracyRate(
        correctDecisionCount: Long,
        settledDecisionCount: Long,
    ): Int {
        if (settledDecisionCount == 0L) return 0
        return (correctDecisionCount.toDouble() * 100 / settledDecisionCount).roundToInt()
    }

    private fun calculateConsecutiveDays(
        latestConsecutiveDays: Int,
        latestAttendanceAt: LocalDateTime?,
        today: LocalDate,
    ): Int {
        val latestAttendanceDate = latestAttendanceAt?.toLocalDate() ?: return 0
        return if (latestAttendanceDate == today || latestAttendanceDate == today.minusDays(1)) {
            latestConsecutiveDays
        } else {
            0
        }
    }

    companion object {
        private val NICKNAME_LENGTH_RANGE = 1..50
        private val COMPANY_NAME_LENGTH_RANGE = 1..100
    }
}
