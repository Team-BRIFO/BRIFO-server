package com.brifo.server.notification.repository

import com.brifo.server.TestcontainersConfiguration
import com.brifo.server.agent.entity.Agent
import com.brifo.server.agent.entity.AgentType
import com.brifo.server.agent.repository.AgentRepository
import com.brifo.server.ap.entity.ApTransaction
import com.brifo.server.ap.entity.ApTransactionReason
import com.brifo.server.ap.entity.ApTransactionTargetType
import com.brifo.server.ap.repository.ApTransactionRepository
import com.brifo.server.briefing.entity.Briefing
import com.brifo.server.briefing.entity.BriefingDirection
import com.brifo.server.briefing.repository.BriefingRepository
import com.brifo.server.decision.entity.Decision
import com.brifo.server.decision.entity.DecisionDirection
import com.brifo.server.decision.entity.DecisionResult
import com.brifo.server.decision.repository.DecisionRepository
import com.brifo.server.decision.repository.DecisionResultRepository
import com.brifo.server.global.config.JpaConfig
import com.brifo.server.global.config.QueryDslConfig
import com.brifo.server.news.entity.ImportanceBadge
import com.brifo.server.news.entity.News
import com.brifo.server.news.entity.NewsCard
import com.brifo.server.news.entity.NewsSource
import com.brifo.server.news.repository.NewsCardRepository
import com.brifo.server.news.repository.NewsRepository
import com.brifo.server.stock.entity.DailyStockPrice
import com.brifo.server.stock.entity.Stock
import com.brifo.server.stock.entity.UserStock
import com.brifo.server.stock.repository.DailyStockPriceRepository
import com.brifo.server.stock.repository.StockRepository
import com.brifo.server.stock.repository.UserStockRepository
import com.brifo.server.user.entity.OAuthProvider
import com.brifo.server.user.entity.User
import com.brifo.server.user.repository.UserRepository
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals

@DataJpaTest
@Import(TestcontainersConfiguration::class, JpaConfig::class, QueryDslConfig::class)
@ActiveProfiles("test")
class NotificationContentQueryRepositoryTest {
    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var stockRepository: StockRepository

    @Autowired
    private lateinit var userStockRepository: UserStockRepository

    @Autowired
    private lateinit var newsRepository: NewsRepository

    @Autowired
    private lateinit var newsCardRepository: NewsCardRepository

    @Autowired
    private lateinit var agentRepository: AgentRepository

    @Autowired
    private lateinit var briefingRepository: BriefingRepository

    @Autowired
    private lateinit var apTransactionRepository: ApTransactionRepository

    @Autowired
    private lateinit var dailyStockPriceRepository: DailyStockPriceRepository

    @Autowired
    private lateinit var decisionRepository: DecisionRepository

    @Autowired
    private lateinit var decisionResultRepository: DecisionResultRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @Test
    fun `카드 2건과 브리핑 3건 구조에서 카드와 의뢰비를 중복 없이 projection한다`() {
        val displayDate = LocalDate.of(2026, 7, 23)
        val user = saveUser()
        val stock = stockRepository.saveAndFlush(Stock.create("005930", "삼성전자", "반도체"))
        userStockRepository.saveAndFlush(UserStock.create(user, stock))
        val cards =
            (1..2).map { index ->
                val news =
                    newsRepository.saveAndFlush(
                        News.create(
                            stock = stock,
                            source = NewsSource.NAVER,
                            sourceUrl = "https://example.com/$index",
                            title = "뉴스 $index",
                            summary = null,
                            importance = null,
                            dedupKey = "notification-news-$index",
                            publishedAt = LocalDateTime.of(2026, 7, 23, 9, index),
                        ),
                    )
                newsCardRepository.saveAndFlush(
                    NewsCard.create(
                        news = news,
                        headline = "뉴스 $index",
                        points = listOf("핵심"),
                        keywords = listOf("삼성전자"),
                        importanceBadge = ImportanceBadge.HOT,
                        displayDate = displayDate,
                    ),
                )
            }
        val agentTypes = listOf(AgentType.ROOKIE, AgentType.PRO, AgentType.TANKER)
        val briefings =
            agentTypes.mapIndexed { index, agentType ->
                val agent =
                    agentRepository.saveAndFlush(
                        Agent.create(
                            user = user,
                            agentType = agentType,
                            modelName = "model-$index",
                            nickname = agentType.name,
                            description = "설명",
                            dailySalary = 10,
                        ),
                    )
                briefingRepository.saveAndFlush(Briefing.create(cards, agent))
            }
        briefings.forEach { briefing ->
            apTransactionRepository.saveAndFlush(
                ApTransaction.salary(
                    user = user,
                    briefingId = requireNotNull(briefing.id),
                    salaryCost = 10,
                ),
            )
        }
        briefings.first().run {
            startAnalysis()
            complete(
                direction = BriefingDirection.DOWN,
                confidenceRate = 65.toShort(),
                contentText = "분석",
                oneLiner = "한 줄",
                headline = "제목",
                summary = "요약",
                personalComment = null,
            )
        }
        val decision =
            decisionRepository.saveAndFlush(
                Decision.create(
                    briefing = briefings.first(),
                    direction = DecisionDirection.UP,
                    confidenceLevel = 3,
                ),
            )
        val dailyPrice =
            dailyStockPriceRepository.saveAndFlush(
                DailyStockPrice.create(
                    stock = stock,
                    tradeDate = displayDate,
                    price = BigDecimal("70000"),
                    changeRate = BigDecimal("1.90"),
                ),
            )
        decisionResultRepository.saveAndFlush(DecisionResult.create(decision, dailyPrice, true))
        apTransactionRepository.saveAndFlush(
            ApTransaction.create(
                user = user,
                amount = 100,
                reason = ApTransactionReason.DECISION_WIN,
                targetType = ApTransactionTargetType.DECISION,
                targetId = requireNotNull(decision.id),
            ),
        )
        entityManager.flush()
        entityManager.clear()

        val userId = requireNotNull(user.publicId)
        val stockId = requireNotNull(stock.publicId)
        val newsCards = notificationRepository.findNewsCardContents(userId, displayDate)
        val salaries = notificationRepository.findAgentSalaryContents(userId, stockId, displayDate)
        val briefing =
            notificationRepository.findBriefingReadyContent(
                userPublicId = userId,
                briefingPublicId = requireNotNull(briefings.first().publicId),
            )
        val decisionResult =
            notificationRepository.findDecisionResultContent(
                userPublicId = userId,
                decisionPublicId = requireNotNull(decision.publicId),
            )

        assertEquals(listOf("삼성전자", "삼성전자"), newsCards.map { it.stockName })
        assertEquals(agentTypes, salaries.map { it.agentType })
        assertEquals(listOf(-10, -10, -10), salaries.map { it.salaryAmount })
        assertEquals("삼성전자", briefing?.stockName)
        assertEquals(BriefingDirection.DOWN, briefing?.direction)
        assertEquals(65.toShort(), briefing?.confidenceRate)
        assertEquals("삼성전자", decisionResult?.stockName)
        assertEquals(BigDecimal("1.90"), decisionResult?.changeRate)
        assertEquals(100, decisionResult?.apAmount)
    }

    private fun saveUser(): User =
        userRepository
            .saveAndFlush(
                User.create(
                    provider = OAuthProvider.KAKAO,
                    socialId = "notification-content-user",
                    email = "notification@example.com",
                ),
            ).also(entityManager::refresh)
}
