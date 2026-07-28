package com.brifo.server.ap.service

import com.brifo.server.agent.exception.AgentNotFoundException
import com.brifo.server.agent.repository.AgentRepository
import com.brifo.server.ap.dto.request.CreateCreditLoanRequest
import com.brifo.server.ap.dto.request.GetApTransactionsRequest
import com.brifo.server.ap.dto.response.ApBalanceResponse
import com.brifo.server.ap.dto.response.CreateAttendanceRewardResponse
import com.brifo.server.ap.dto.response.GetApTransactionsResponse
import com.brifo.server.ap.entity.ApTransactionReason
import com.brifo.server.ap.entity.ApTransactionTargetType
import com.brifo.server.ap.entity.AttendanceReward
import com.brifo.server.ap.exception.AttendanceRewardAlreadyClaimedException
import com.brifo.server.ap.exception.CreditLoanAlreadyClaimedException
import com.brifo.server.ap.exception.CreditLoanNotEligibleException
import com.brifo.server.ap.exception.TutorialRewardAlreadyClaimedException
import com.brifo.server.ap.repository.ApTransactionRepository
import com.brifo.server.ap.repository.AttendanceRewardRepository
import com.brifo.server.badge.code.BadgeCode
import com.brifo.server.badge.service.BadgeAwardService
import com.brifo.server.global.common.CursorPage
import com.brifo.server.notification.entity.NotificationCode
import com.brifo.server.notification.entity.NotificationTargetType
import com.brifo.server.notification.service.NotificationCreationService
import com.brifo.server.user.entity.User
import com.brifo.server.user.exception.UserNotFoundException
import com.brifo.server.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

@Service
class ApService(
    private val apTransactionService: ApTransactionService,
    private val apTransactionRepository: ApTransactionRepository,
    private val attendanceRewardRepository: AttendanceRewardRepository,
    private val agentRepository: AgentRepository,
    private val userRepository: UserRepository,
    private val badgeAwardService: BadgeAwardService,
    private val notificationCreationService: NotificationCreationService,
    private val clock: Clock,
) {
    @Transactional(readOnly = true)
    fun getApTransactions(
        userPublicId: UUID,
        request: GetApTransactionsRequest,
    ): GetApTransactionsResponse {
        val user = userRepository.findByPublicId(userPublicId) ?: throw UserNotFoundException()
        val userId = requireNotNull(user.id)
        val today = LocalDate.now(clock)

        return GetApTransactionsResponse(
            summary = getApSummary(user, today),
            page = getApTransactionPage(userId, request),
        )
    }

    private fun getApSummary(
        user: User,
        today: LocalDate,
    ): GetApTransactionsResponse.Summary {
        val monthStart = today.withDayOfMonth(1).atStartOfDay()
        val nextMonthStart = monthStart.plusMonths(1)
        val monthlyAmounts =
            apTransactionRepository.findMonthlyAmountsByUserId(
                userId = requireNotNull(user.id),
                monthStart = monthStart,
                nextMonthStart = nextMonthStart,
            )

        return GetApTransactionsResponse.Summary(
            balanceAp = user.balanceAp,
            monthlyEarnedAp = monthlyAmounts.earnedAp,
            monthlyLostAp = monthlyAmounts.lostAp,
        )
    }

    private fun getApTransactionPage(
        userId: Long,
        request: GetApTransactionsRequest,
    ): CursorPage<GetApTransactionsResponse.Item> {
        val transactions =
            apTransactionRepository.findPageByUserId(
                userId = userId,
                cursor = request.cursor,
                limit = request.size + 1,
            )
        val hasNext = transactions.size > request.size
        val pageItems = transactions.take(request.size)

        return CursorPage(
            items = pageItems,
            nextCursor = if (hasNext) pageItems.last().apTransactionId else null,
            hasNext = hasNext,
        )
    }

    @Transactional
    fun createAttendanceReward(userId: UUID): CreateAttendanceRewardResponse {
        val user = userRepository.findForUpdateByPublicId(userId) ?: throw UserNotFoundException()
        val internalUserId = requireNotNull(user.id)

        // 연속 출석일 계산
        val consecutiveDays = determineConsecutiveDays(
            lastReward = attendanceRewardRepository.findTopByUserIdOrderByCreatedAtDesc(internalUserId),
            today = LocalDate.now(clock)
        )

        // 출석 기록 생성
        val attendanceReward = attendanceRewardRepository.save(AttendanceReward.create(user, consecutiveDays))
        attendanceRewardRepository.flush()

        // 연속 출석 보너스 여부 확인
        val bonusRewarded = consecutiveDays == ATTENDANCE_CYCLE_DAYS
        val rewardedAp = ATTENDANCE_REWARD_AP + if (bonusRewarded) ATTENDANCE_BONUS_AP else 0

        // AP 원장 기록 생성
        val balanceAp =
            apTransactionService.change(
                userId = userId,
                deltaAp = rewardedAp,
                reason = ApTransactionReason.ATTENDANCE,
                target =
                    ApTransactionService.Target(
                        type = ApTransactionTargetType.ATTENDANCE_REWARD,
                        id = requireNotNull(attendanceReward.id),
                    ),
            )

        if (consecutiveDays >= THREE_DAY_ATTENDANCE_BADGE_DAYS) {
            badgeAwardService.awardBadge(userId, BadgeCode.B05)
        }
        if (consecutiveDays >= ATTENDANCE_CYCLE_DAYS) {
            badgeAwardService.awardBadge(userId, BadgeCode.B06)
        }
        notificationCreationService.create(
            userId = userId,
            code = NotificationCode.ATTENDANCE_REWARDED,
            target =
                NotificationCreationService.Target(
                    type = NotificationTargetType.NONE,
                    id = null,
                ),
            eventId = requireNotNull(attendanceReward.id),
        )

        return CreateAttendanceRewardResponse(rewardedAp, bonusRewarded, consecutiveDays, balanceAp)
    }

    private fun determineConsecutiveDays(
        lastReward: AttendanceReward?,
        today: LocalDate,
    ): Int {
        val reward = lastReward ?: return 1
        val lastRewardDate = requireNotNull(reward.createdAt).toLocalDate()

        if (lastRewardDate == today) {
            throw AttendanceRewardAlreadyClaimedException()
        }

        val isConsecutive =
            lastRewardDate == today.minusDays(1) &&
                reward.consecutiveDays < ATTENDANCE_CYCLE_DAYS

        return if (isConsecutive) reward.consecutiveDays + 1 else 1
    }

    @Transactional
    fun createTutorialReward(userId: UUID): ApBalanceResponse {
        val user = userRepository.findForUpdateByPublicId(userId) ?: throw UserNotFoundException()
        val internalUserId = requireNotNull(user.id)

        if (apTransactionRepository.existsByUserIdAndReason(internalUserId, ApTransactionReason.TUTORIAL)) {
            throw TutorialRewardAlreadyClaimedException()
        }
        val balanceAp = apTransactionService.change(userId, TUTORIAL_REWARD_AP, ApTransactionReason.TUTORIAL)
        badgeAwardService.awardBadge(userId, BadgeCode.B01)
        return ApBalanceResponse(balanceAp)
    }

    @Transactional
    fun createCreditLoan(
        userId: UUID,
        request: CreateCreditLoanRequest,
    ): ApBalanceResponse {
        val user = userRepository.findForUpdateByPublicId(userId) ?: throw UserNotFoundException()
        val internalUserId = requireNotNull(user.id)
        val agent = agentRepository.findByPublicId(request.agentId)
        if (agent == null || agent.user.id != internalUserId) {
            throw AgentNotFoundException()
        }
        if (apTransactionRepository.existsByUserIdAndReason(internalUserId, ApTransactionReason.CREDIT_LOAN)) {
            throw CreditLoanAlreadyClaimedException()
        }
        if (user.balanceAp >= agent.dailySalary) {
            throw CreditLoanNotEligibleException()
        }
        val balanceAp = apTransactionService.change(userId, CREDIT_LOAN_AP, ApTransactionReason.CREDIT_LOAN)
        return ApBalanceResponse(balanceAp)
    }

    companion object {
        private const val ATTENDANCE_REWARD_AP = 50
        private const val ATTENDANCE_BONUS_AP = 200
        private const val THREE_DAY_ATTENDANCE_BADGE_DAYS = 3
        private const val ATTENDANCE_CYCLE_DAYS = 7
        private const val TUTORIAL_REWARD_AP = 200
        private const val CREDIT_LOAN_AP = 200
    }
}
