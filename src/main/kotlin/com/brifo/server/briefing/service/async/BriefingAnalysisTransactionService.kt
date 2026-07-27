package com.brifo.server.briefing.service.async

import com.brifo.server.ap.entity.ApTransactionReason
import com.brifo.server.ap.entity.ApTransactionTargetType
import com.brifo.server.ap.repository.ApTransactionRepository
import com.brifo.server.ap.service.ApTransactionService
import com.brifo.server.briefing.entity.BriefingStatus
import com.brifo.server.briefing.exception.BriefingNotFoundException
import com.brifo.server.briefing.repository.BriefingRepository
import com.brifo.server.decision.repository.DecisionRepository
import com.brifo.server.user.exception.UserNotFoundException
import com.brifo.server.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.RoundingMode
import java.util.UUID

/** 비동기 분석 전후의 브리핑 상태 변경과 AP 환불을 짧은 트랜잭션으로 처리한다. */
@Service
class BriefingAnalysisTransactionService(
    private val briefingRepository: BriefingRepository,
    private val decisionRepository: DecisionRepository,
    private val userRepository: UserRepository,
    private val apTransactionRepository: ApTransactionRepository,
    private val apTransactionService: ApTransactionService,
) {
    @Transactional
    fun start(command: BriefingAnalysisTask.Command): BriefingAnalysisTask.Context? {
        val briefings = command.briefingPublicIds
            .sorted()
            .map { briefingPublicId ->
                // 요청한 브리핑이 존재하는지 검증하면서 항상 같은 순서로 행을 잠근다.
                briefingRepository.findForUpdateByPublicId(briefingPublicId)
                    ?: throw BriefingNotFoundException()
            }
        // 요청 묶음의 모든 브리핑이 분석 시작 전 상태인지 검증한다.
        if (briefings.any { it.status != BriefingStatus.PENDING }) {
            return null
        }
        // 다른 사용자의 브리핑을 분석 요청에 포함하지 않았는지 검증한다.
        if (briefings.any { it.agent.user.publicId != command.userPublicId }) {
            throw BriefingNotFoundException()
        }

        briefings.forEach { it.startAnalysis() }
        val newsCards = briefings
            .flatMap { it.newsCards }
            .distinctBy { it.publicId }

        return BriefingAnalysisTask.Context(
            userPublicId = command.userPublicId,
            newsCardPublicIds = newsCards.map { newsCard ->
                checkNotNull(newsCard.publicId) { "Persisted news card must have a publicId" }
            },
            targets = briefings.map { briefing ->
                BriefingAnalysisTask.Context.Target(
                    briefingPublicId = checkNotNull(briefing.publicId) {
                        "Persisted briefing must have a publicId"
                    },
                    agentPublicId = checkNotNull(briefing.agent.publicId) {
                        "Persisted agent must have a publicId"
                    },
                    agentType = briefing.agent.agentType,
                    modelName = briefing.agent.modelName,
                )
            },
            recentDecisionPublicIds = decisionRepository.findRecentSettledDecisionIds(
                userPublicId = command.userPublicId,
                limit = RECENT_DECISION_LIMIT,
            ),
        )
    }

    @Transactional
    fun complete(completion: BriefingAnalysisTask.Completion) {
        // 완료 대상 브리핑이 존재하는지 검증하면서 중복 완료를 막기 위해 행을 잠근다.
        val briefing = briefingRepository.findForUpdateByPublicId(completion.briefingPublicId)
            ?: throw BriefingNotFoundException()
        // 외부 분석을 시작한 상태인 경우에만 결과를 반영한다.
        if (briefing.status != BriefingStatus.ANALYZING) {
            return
        }

        val confidenceRate = completion.probability
            .multiply(ONE_HUNDRED)
            .setScale(0, RoundingMode.HALF_UP)
            .shortValueExact()
        briefing.complete(
            direction = completion.direction,
            confidenceRate = confidenceRate,
            contentText = completion.commonAnalysis,
            oneLiner = completion.closingComment,
            headline = completion.headline,
            summary = completion.summary,
            personalComment = completion.personalComment,
        )
    }

    @Transactional
    fun failAndRefund(briefingPublicId: UUID) {
        // 실패 대상 브리핑과 소유 사용자가 존재하는지 검증한다.
        val ownerPublicId = briefingRepository.findOwnerPublicId(briefingPublicId)
            ?: throw BriefingNotFoundException()
        // AP 환불을 직렬화하기 위해 사용자 행을 먼저 잠근다.
        userRepository.findForUpdateByPublicId(ownerPublicId) ?: throw UserNotFoundException()
        // 중복 실패와 환불을 막기 위해 브리핑 행을 잠근다.
        val briefing = briefingRepository.findForUpdateByPublicId(briefingPublicId)
            ?: throw BriefingNotFoundException()

        // 외부 분석을 시작한 상태인 경우에만 실패와 환불을 수행한다.
        if (briefing.status != BriefingStatus.ANALYZING) {
            return
        }

        val briefingId = checkNotNull(briefing.id) { "Persisted briefing must have an id" }
        val salaryBalance = apTransactionRepository.sumBriefingSalaryBalance(briefingId)
        // 최초 요청이나 재시도에서 기록한 환불 대상 급여 거래가 남아 있는지 검증한다.
        check(salaryBalance < 0) { "Salary transaction is missing for briefing $briefingPublicId" }

        val refundAmount = -salaryBalance
        briefing.fail()
        apTransactionService.change(
            userId = ownerPublicId,
            deltaAp = refundAmount,
            reason = ApTransactionReason.SALARY_REFUND,
            target =
                ApTransactionService.Target(
                    type = ApTransactionTargetType.BRIEFING,
                    id = briefingId,
                ),
        )
    }

    private companion object {
        const val RECENT_DECISION_LIMIT = 3L
        val ONE_HUNDRED = 100.toBigDecimal()
    }
}
