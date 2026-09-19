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
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate

/**
 * 게스트 체험 계정이 처음부터 빈 화면을 보지 않도록, 이미 종가가 확정된 과거 날짜의
 * 실제 뉴스카드를 재사용해 완결된 예측(적중/실패)과 결정일기를 즉시 만들어준다.
 * 정산 로직은 실제 배치가 쓰는 DecisionSettlementService를 그대로 재사용해
 * AP 지급·배지·경험치·알림까지 실제 서비스와 동일하게 처리한다.
 *
 * 날짜를 고정해두면 운영 DB에 그 날짜의 뉴스카드·종가가 보존되어 있다는 보장이 없어
 * 온보딩이 끝나도 목데이터가 비는 경우가 생긴다. 그래서 기준일(어제) 이전으로
 * 하루씩 거슬러 올라가며 뉴스카드와 종가가 모두 존재하는 날짜를 찾아 채운다.
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
    private val clock: Clock,
) {
    @Transactional
    fun seed(user: User) {
        val agents = agentRepository.findAllByUserId(requireNotNull(user.id))
        if (agents.isEmpty()) return

        var seeded = 0
        var date = LocalDate.now(clock).minusDays(1)
        var attempts = 0
        while (seeded < TARGET_COUNT && attempts < MAX_LOOKBACK_DAYS) {
            val correct = OUTCOMES[seeded % OUTCOMES.size]
            if (seedOne(user, agents[seeded % agents.size], date, correct)) {
                seeded++
            }
            date = date.minusDays(1)
            attempts++
        }

        if (seeded < TARGET_COUNT) {
            log.warn(
                "게스트 목데이터 시딩: {}건 중 {}건만 채워짐(뉴스카드·종가가 있는 과거 날짜 부족). userPublicId={}",
                TARGET_COUNT,
                seeded,
                user.publicId,
            )
        }
    }

    /** 해당 날짜에 뉴스카드와 종가가 모두 있으면 결정일기 1건을 만들고 true를 반환한다. */
    private fun seedOne(
        user: User,
        agent: Agent,
        date: LocalDate,
        correct: Boolean,
    ): Boolean {
        val newsCard = newsCardRepository.findFirstByDisplayDateOrderByIdAsc(date) ?: return false
        val stock = newsCard.news.stock
        val closingPrice =
            dailyStockPriceRepository.findByStockIdAndTradeDateAndIsClosingTrue(
                requireNotNull(stock.id),
                date,
            ) ?: return false

        val actualDirection = calculator.actualDirection(closingPrice.changeRate)
        val predictedDirection = if (correct) actualDirection else wrongDirection(actualDirection)

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
                isCorrect = correct,
            ),
        )
        return true
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

    private companion object {
        val log = LoggerFactory.getLogger(GuestMockDataSeedingService::class.java)

        const val ALLOCATED_AP = 50_000
        const val ALLOCATION_RATE_PERCENT = 5
        const val CONFIDENCE_RATE: Short = 75
        const val TARGET_COUNT = 3
        const val MAX_LOOKBACK_DAYS = 30

        /** 채워지는 순서대로 2적중 1실패가 되도록 한다. */
        val OUTCOMES = listOf(true, false, true)
    }
}
