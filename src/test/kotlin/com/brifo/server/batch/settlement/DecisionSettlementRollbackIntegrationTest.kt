package com.brifo.server.batch.settlement

import com.brifo.server.ServerTestConfiguration
import com.brifo.server.batch.anyKotlin
import com.brifo.server.agent.entity.Agent
import com.brifo.server.agent.entity.AgentType
import com.brifo.server.agent.repository.AgentRepository
import com.brifo.server.ap.repository.ApTransactionRepository
import com.brifo.server.badge.repository.UserBadgeRepository
import com.brifo.server.briefing.entity.Briefing
import com.brifo.server.briefing.repository.BriefingRepository
import com.brifo.server.decision.entity.Decision
import com.brifo.server.decision.entity.DecisionDirection
import com.brifo.server.decision.repository.DecisionRepository
import com.brifo.server.decision.repository.DecisionResultRepository
import com.brifo.server.diary.repository.DiaryEntryRepository
import com.brifo.server.news.entity.ImportanceBadge
import com.brifo.server.news.entity.News
import com.brifo.server.news.entity.NewsCard
import com.brifo.server.news.entity.NewsSource
import com.brifo.server.news.repository.NewsCardRepository
import com.brifo.server.news.repository.NewsRepository
import com.brifo.server.notification.service.NotificationCreationService
import com.brifo.server.stock.entity.DailyStockPrice
import com.brifo.server.stock.entity.Stock
import com.brifo.server.stock.repository.DailyStockPriceRepository
import com.brifo.server.stock.repository.StockRepository
import com.brifo.server.user.entity.OAuthProvider
import com.brifo.server.user.entity.User
import com.brifo.server.user.repository.UserRepository
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.isNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

@Import(ServerTestConfiguration::class)
@ActiveProfiles("test")
@SpringBootTest
class DecisionSettlementRollbackIntegrationTest {
    @Autowired private lateinit var settlementService: DecisionSettlementService
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var stockRepository: StockRepository
    @Autowired private lateinit var newsRepository: NewsRepository
    @Autowired private lateinit var newsCardRepository: NewsCardRepository
    @Autowired private lateinit var agentRepository: AgentRepository
    @Autowired private lateinit var briefingRepository: BriefingRepository
    @Autowired private lateinit var decisionRepository: DecisionRepository
    @Autowired private lateinit var decisionResultRepository: DecisionResultRepository
    @Autowired private lateinit var dailyStockPriceRepository: DailyStockPriceRepository
    @Autowired private lateinit var apTransactionRepository: ApTransactionRepository
    @Autowired private lateinit var diaryEntryRepository: DiaryEntryRepository
    @Autowired private lateinit var userBadgeRepository: UserBadgeRepository
    @Autowired private lateinit var entityManager: EntityManager

    @MockitoBean
    private lateinit var notificationCreationService: NotificationCreationService

    @Test
    fun `알림 생성이 실패하면 결정 한 건의 모든 정산 결과를 롤백한다`() {
        val transactionCount = apTransactionRepository.count()
        val badgeCount = userBadgeRepository.count()
        val diaryCount = diaryEntryRepository.count()
        val fixture = createFixture()
        doThrow(IllegalStateException("notification failed"))
            .`when`(notificationCreationService)
            .create(anyKotlin(), anyKotlin(), anyKotlin(), isNull(), isNull())

        assertFailsWith<IllegalStateException> {
            settlementService.settle(
                DecisionSettlementItem(
                    decisionId = fixture.decisionId,
                    dailyStockPriceId = fixture.priceId,
                    isCorrect = true,
                ),
            )
        }
        entityManager.clear()

        assertFalse(decisionResultRepository.existsByDecisionId(fixture.decisionId))
        assertEquals(diaryCount, diaryEntryRepository.count())
        assertEquals(transactionCount, apTransactionRepository.count())
        assertEquals(badgeCount, userBadgeRepository.count())
        assertEquals(0, agentRepository.findById(fixture.agentId).orElseThrow().exp)
        assertEquals(0, userRepository.findById(fixture.userId).orElseThrow().balanceAp)
    }

    private fun createFixture(): Fixture {
        val suffix = UUID.randomUUID().toString().take(6)
        val user = userRepository.save(User.create(OAuthProvider.KAKAO, "batch-$suffix", "batch-$suffix@example.com"))
        val stock = stockRepository.save(Stock.create("B$suffix", "브리포주식", "금융"))
        val news = newsRepository.save(
            News.create(
                stock = stock,
                source = NewsSource.TEST,
                sourceUrl = "https://example.com/news",
                title = "뉴스",
                summary = "요약",
                importance = BigDecimal("1.00"),
                dedupKey = "settlement-rollback-news-$suffix",
                publishedAt = LocalDateTime.of(2026, 8, 3, 11, 0),
            ),
        )
        val card = newsCardRepository.save(
            NewsCard.create(
                news = news,
                headline = "카드",
                points = listOf("포인트"),
                keywords = listOf("키워드"),
                importanceBadge = ImportanceBadge.HOT,
                displayDate = LocalDate.of(2026, 8, 4),
            ),
        )
        val agent = agentRepository.save(Agent.create(user, AgentType.ROOKIE, "model", "루키", "설명", 10))
        val briefing = briefingRepository.save(Briefing.create(listOf(card), agent))
        val decision = decisionRepository.save(Decision.create(briefing, DecisionDirection.UP, 5))
        val price = dailyStockPriceRepository.save(
            DailyStockPrice.createClosing(
                stock = stock,
                tradeDate = LocalDate.of(2026, 8, 4),
                price = BigDecimal("1050.00"),
                changeRate = BigDecimal("0.50"),
            ),
        )
        return Fixture(
            userId = requireNotNull(user.id),
            agentId = requireNotNull(agent.id),
            decisionId = requireNotNull(decision.id),
            priceId = requireNotNull(price.id),
        )
    }

    private data class Fixture(
        val userId: Long,
        val agentId: Long,
        val decisionId: Long,
        val priceId: Long,
    )
}
