package com.brifo.server.auth.dev

import com.brifo.server.agent.entity.AgentType
import com.brifo.server.agent.repository.AgentRepository
import com.brifo.server.ap.entity.ApTransactionReason
import com.brifo.server.ap.entity.ApTransactionTargetType
import com.brifo.server.ap.service.ApService
import com.brifo.server.ap.service.ApTransactionService
import com.brifo.server.badge.code.BadgeCode
import com.brifo.server.badge.service.BadgeAwardService
import com.brifo.server.briefing.entity.Briefing
import com.brifo.server.briefing.entity.BriefingDirection
import com.brifo.server.briefing.repository.BriefingRepository
import com.brifo.server.decision.entity.Decision
import com.brifo.server.decision.entity.DecisionDirection
import com.brifo.server.decision.entity.DecisionResult
import com.brifo.server.decision.repository.DecisionRepository
import com.brifo.server.decision.repository.DecisionResultRepository
import com.brifo.server.diary.entity.DiaryEntry
import com.brifo.server.diary.repository.DiaryEntryRepository
import com.brifo.server.news.repository.NewsCardRepository
import com.brifo.server.notification.entity.NotificationCode
import com.brifo.server.notification.entity.NotificationTargetType
import com.brifo.server.notification.service.NotificationCreationService
import com.brifo.server.stock.repository.DailyStockPriceRepository
import com.brifo.server.stock.repository.StockRepository
import com.brifo.server.term.repository.GlossaryTermRepository
import com.brifo.server.term.repository.UserLearnedTermRepository
import com.brifo.server.user.repository.UserRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
@Profile("dev")
@ConditionalOnProperty(prefix = "app.dev-auth", name = ["enabled"], havingValue = "true")
class DevOnboardingDataService(
    private val userRepository: UserRepository,
    private val agentRepository: AgentRepository,
    private val stockRepository: StockRepository,
    private val dailyStockPriceRepository: DailyStockPriceRepository,
    private val newsCardRepository: NewsCardRepository,
    private val briefingRepository: BriefingRepository,
    private val decisionRepository: DecisionRepository,
    private val decisionResultRepository: DecisionResultRepository,
    private val diaryEntryRepository: DiaryEntryRepository,
    private val apService: ApService,
    private val apTransactionService: ApTransactionService,
    private val badgeAwardService: BadgeAwardService,
    private val notificationCreationService: NotificationCreationService,
    private val glossaryTermRepository: GlossaryTermRepository,
    private val userLearnedTermRepository: UserLearnedTermRepository,
) {
    @Transactional
    fun seed(userPublicId: UUID) {
        val user = checkNotNull(userRepository.findByPublicId(userPublicId))
        check(decisionRepository.countByBriefingAgentUserId(requireNotNull(user.id)) == 0L) {
            "Dev onboarding data already exists"
        }

        apService.createTutorialReward(userPublicId)
        apService.createAttendanceReward(userPublicId)

        val agents =
            agentRepository
                .findAllByUserPublicIdOrderByAgentTypeAsc(userPublicId)
                .associateBy { it.agentType }
        check(agents.keys.containsAll(SCENARIOS.map { it.agentType })) { "Missing onboarding agents" }

        SCENARIOS.forEach { scenario ->
            val stock = checkNotNull(stockRepository.findByCode(scenario.stockCode))
            val stockPublicId = requireNotNull(stock.publicId)
            val cards = newsCardRepository.findAnalysisCards(stockPublicId, DEV_DISPLAY_DATE)
            check(cards.isNotEmpty()) { "Missing dev news cards for ${scenario.stockCode}" }
            val price =
                checkNotNull(
                    dailyStockPriceRepository.findTopByStockIdOrderByTradeDateDescFetchedAtDescIdDesc(
                        requireNotNull(stock.id),
                    ),
                ) { "Missing dev stock prices for ${scenario.stockCode}" }

            val briefing = Briefing.create(cards, checkNotNull(agents[scenario.agentType]))
            briefing.startAnalysis()
            briefing.complete(
                direction = scenario.briefingDirection,
                confidenceRate = scenario.confidenceRate,
                contentText = "${stock.name}의 테스트 브리핑 본문입니다.",
                oneLiner = "${stock.name}의 흐름을 확인해 보세요.",
                headline = "${stock.name} 테스트 브리핑",
                summary = "프론트 화면 확인을 위한 ${stock.name} 요약입니다.",
                personalComment = "테스트 데이터에 기반한 의견입니다.",
            )
            briefingRepository.saveAndFlush(briefing)

            apTransactionService.change(
                userId = userPublicId,
                deltaAp = -scenario.salary,
                reason = ApTransactionReason.SALARY,
                target =
                    ApTransactionService.Target(
                        type = ApTransactionTargetType.BRIEFING,
                        id = requireNotNull(briefing.id),
                    ),
            )

            val decision =
                decisionRepository.saveAndFlush(
                    Decision.create(
                        briefing = briefing,
                        direction = scenario.decisionDirection,
                        confidenceLevel = scenario.confidenceLevel,
                    ),
                )
            decisionResultRepository.saveAndFlush(
                DecisionResult.create(
                    decision = decision,
                    dailyStockPrice = price,
                    isCorrect = scenario.isCorrect,
                ),
            )
            diaryEntryRepository.save(DiaryEntry.create(decision))

            apTransactionService.change(
                userId = userPublicId,
                deltaAp = scenario.resultAp,
                reason = scenario.resultReason,
                target =
                    ApTransactionService.Target(
                        type = ApTransactionTargetType.DECISION,
                        id = requireNotNull(decision.id),
                    ),
            )
            notificationCreationService.create(
                userId = userPublicId,
                code = NotificationCode.DECISION_RESULT,
                target =
                    NotificationCreationService.Target(
                        type = NotificationTargetType.DECISION,
                        id = requireNotNull(decision.publicId),
                    ),
            )
        }

        badgeAwardService.awardBadge(userPublicId, BadgeCode.B02)
        badgeAwardService.awardBadge(userPublicId, BadgeCode.B03)
        badgeAwardService.awardBadge(userPublicId, BadgeCode.B04)
        learnTerms(userPublicId)
    }

    private fun learnTerms(userPublicId: UUID) {
        val userId = requireNotNull(userRepository.findByPublicId(userPublicId)?.id)
        glossaryTermRepository
            .findAll()
            .take(3)
            .forEach { term ->
                userLearnedTermRepository.insertIfAbsent(userId, requireNotNull(term.id))
            }
    }

    private data class Scenario(
        val agentType: AgentType,
        val stockCode: String,
        val briefingDirection: BriefingDirection,
        val decisionDirection: DecisionDirection,
        val confidenceRate: Short,
        val confidenceLevel: Int,
        val isCorrect: Boolean,
        val salary: Int,
        val resultAp: Int,
        val resultReason: ApTransactionReason,
    )

    private companion object {
        val DEV_DISPLAY_DATE: LocalDate = LocalDate.of(2026, 8, 2)
        val SCENARIOS =
            listOf(
                Scenario(
                    AgentType.ROOKIE,
                    "BRIFO01",
                    BriefingDirection.UP,
                    DecisionDirection.UP,
                    82,
                    5,
                    true,
                    10,
                    100,
                    ApTransactionReason.DECISION_WIN,
                ),
                Scenario(
                    AgentType.PRO,
                    "BRIFO02",
                    BriefingDirection.DOWN,
                    DecisionDirection.DOWN,
                    71,
                    3,
                    true,
                    15,
                    100,
                    ApTransactionReason.DECISION_WIN,
                ),
                Scenario(
                    AgentType.TANKER,
                    "BRIFO03",
                    BriefingDirection.NEUTRAL,
                    DecisionDirection.UP,
                    55,
                    4,
                    false,
                    25,
                    -50,
                    ApTransactionReason.DECISION_LOSE,
                ),
            )
    }
}
