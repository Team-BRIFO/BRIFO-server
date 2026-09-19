package com.brifo.server.auth.service

import com.brifo.server.agent.entity.Agent
import com.brifo.server.agent.repository.AgentRepository
import com.brifo.server.ap.entity.ApTransactionReason
import com.brifo.server.ap.entity.ApTransactionTargetType
import com.brifo.server.ap.service.ApTransactionService
import com.brifo.server.badge.code.BadgeCode
import com.brifo.server.badge.service.BadgeAwardService
import com.brifo.server.batch.settlement.DecisionSettlementCalculator
import com.brifo.server.batch.settlement.DecisionSettlementItem
import com.brifo.server.batch.settlement.DecisionSettlementService
import com.brifo.server.briefing.entity.Briefing
import com.brifo.server.briefing.entity.BriefingDirection
import com.brifo.server.briefing.repository.BriefingRepository
import com.brifo.server.decision.entity.Decision
import com.brifo.server.decision.entity.DecisionDirection
import com.brifo.server.decision.repository.DecisionRepository
import com.brifo.server.news.entity.NewsCard
import com.brifo.server.news.repository.NewsCardRepository
import com.brifo.server.stock.entity.Stock
import com.brifo.server.stock.repository.DailyStockPriceRepository
import com.brifo.server.user.entity.User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * 게스트 체험 계정이 처음부터 빈 화면을 보지 않도록, 이미 종가가 확정된 과거 날짜의
 * 실제 뉴스카드를 재사용해 완결된 예측(적중/실패)과 결정일기를 즉시 만들어준다.
 * 정산 로직은 실제 배치가 쓰는 DecisionSettlementService를 그대로 재사용해
 * AP 지급·배지·경험치·알림까지 실제 서비스와 동일하게 처리한다.
 */
@Service
class GuestMockDataSeedingService(
    private val newsCardRepository: NewsCardRepository,
    private val dailyStockPriceRepository: DailyStockPriceRepository,
    private val agentRepository: AgentRepository,
    private val briefingRepository: BriefingRepository,
    private val decisionRepository: DecisionRepository,
    private val apTransactionService: ApTransactionService,
    private val badgeAwardService: BadgeAwardService,
    private val decisionSettlementService: DecisionSettlementService,
    private val calculator: DecisionSettlementCalculator,
) {
    @Transactional
    fun seed(user: User) {
        val agents = agentRepository.findAllByUserId(requireNotNull(user.id))
        if (agents.isEmpty()) return

        MOCK_SCENARIOS.forEachIndexed { index, scenario ->
            seedOne(user, agents[index % agents.size], scenario)
        }
    }

    private fun seedOne(
        user: User,
        agent: Agent,
        scenario: MockScenario,
    ) {
        val newsCard = newsCardRepository.findFirstByDisplayDateOrderByIdAsc(scenario.date) ?: return
        val stock = newsCard.news.stock
        val closingPrice =
            dailyStockPriceRepository.findByStockIdAndTradeDateAndIsClosingTrue(
                requireNotNull(stock.id),
                scenario.date,
            ) ?: return

        val actualDirection = calculator.actualDirection(closingPrice.changeRate)
        val predictedDirection = if (scenario.correct) actualDirection else wrongDirection(actualDirection)

        val briefing = Briefing.create(listOf(newsCard), agent)
        briefing.startAnalysis()
        briefing.complete(
            direction = BriefingDirection.valueOf(predictedDirection.name),
            confidenceRate = CONFIDENCE_RATE,
            contentText = buildContentText(stock, newsCard, predictedDirection),
            oneLiner = "${stock.name} ${directionLabel(predictedDirection)} 전망",
            headline = "${stock.name}, ${directionLabel(predictedDirection)} 흐름 예상",
            summary = "${newsCard.headline} 이슈를 검토했을 때 ${directionLabel(predictedDirection)} 가능성이 높다고 판단했습니다.",
            personalComment = null,
        )
        briefingRepository.save(briefing)

        val decision =
            decisionRepository.save(
                Decision.create(
                    briefing = briefing,
                    direction = predictedDirection,
                    allocatedAp = ALLOCATED_AP,
                    allocationRatePercent = ALLOCATION_RATE_PERCENT,
                ),
            )
        val decisionId = requireNotNull(decision.id)
        val userPublicId = requireNotNull(user.publicId)

        apTransactionService.change(
            userId = userPublicId,
            deltaAp = -ALLOCATED_AP,
            reason = ApTransactionReason.DECISION_ENTRY_FEE,
            target = ApTransactionService.Target(ApTransactionTargetType.DECISION, decisionId),
        )
        badgeAwardService.awardBadge(userPublicId, BadgeCode.B02)

        decisionSettlementService.settle(
            DecisionSettlementItem(
                decisionId = decisionId,
                dailyStockPriceId = requireNotNull(closingPrice.id),
                isCorrect = scenario.correct,
            ),
        )
    }

    private fun buildContentText(
        stock: Stock,
        newsCard: NewsCard,
        direction: DecisionDirection,
    ): String {
        val point = newsCard.points.firstOrNull()
        val pointText = if (point != null) " 핵심 포인트는 '$point'입니다." else ""
        return "${stock.name} 관련 소식 '${newsCard.headline}'을 검토한 결과, " +
            "${directionLabel(direction)} 가능성이 높다고 판단했습니다.$pointText"
    }

    private fun directionLabel(direction: DecisionDirection): String =
        when (direction) {
            DecisionDirection.UP -> "상승"
            DecisionDirection.DOWN -> "하락"
            DecisionDirection.NEUTRAL -> "보합"
        }

    private fun wrongDirection(actual: DecisionDirection): DecisionDirection =
        when (actual) {
            DecisionDirection.UP -> DecisionDirection.DOWN
            DecisionDirection.DOWN -> DecisionDirection.UP
            DecisionDirection.NEUTRAL -> DecisionDirection.UP
        }

    private data class MockScenario(
        val date: LocalDate,
        val correct: Boolean,
    )

    companion object {
        private const val ALLOCATED_AP = 50_000
        private const val ALLOCATION_RATE_PERCENT = 5
        private const val CONFIDENCE_RATE: Short = 75

        /** 9/16 적중, 9/17 실패, 9/18 적중 순서로 결정일기 3건을 만든다. */
        private val MOCK_SCENARIOS =
            listOf(
                MockScenario(LocalDate.of(2026, 9, 16), correct = true),
                MockScenario(LocalDate.of(2026, 9, 17), correct = false),
                MockScenario(LocalDate.of(2026, 9, 18), correct = true),
            )
    }
}
