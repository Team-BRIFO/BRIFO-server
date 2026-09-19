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
import com.brifo.server.briefing.repository.BriefingRepository
import com.brifo.server.decision.entity.Decision
import com.brifo.server.decision.repository.DecisionRepository
import com.brifo.server.news.entity.News
import com.brifo.server.news.entity.NewsCard
import com.brifo.server.news.repository.NewsCardRepository
import com.brifo.server.stock.entity.DailyStockPrice
import com.brifo.server.stock.entity.Stock
import com.brifo.server.stock.repository.DailyStockPriceRepository
import com.brifo.server.user.entity.OAuthProvider
import com.brifo.server.user.entity.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.test.util.ReflectionTestUtils
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class GuestMockDataSeedingServiceTest {
    private val newsCardRepository = mock(NewsCardRepository::class.java)
    private val dailyStockPriceRepository = mock(DailyStockPriceRepository::class.java)
    private val agentRepository = mock(AgentRepository::class.java)
    private val briefingRepository = mock(BriefingRepository::class.java)
    private val decisionRepository = mock(DecisionRepository::class.java)
    private val apTransactionService = mock(ApTransactionService::class.java)
    private val badgeAwardService = mock(BadgeAwardService::class.java)
    private val decisionSettlementService = mock(DecisionSettlementService::class.java)
    private val calculator = DecisionSettlementCalculator()

    private lateinit var service: GuestMockDataSeedingService

    @BeforeEach
    fun setUp() {
        service =
            GuestMockDataSeedingService(
                newsCardRepository,
                dailyStockPriceRepository,
                agentRepository,
                briefingRepository,
                decisionRepository,
                apTransactionService,
                badgeAwardService,
                decisionSettlementService,
                calculator,
            )
    }

    @Test
    fun `게스트의 9월 16~18일 뉴스카드와 종가로 3건을 각각 적중 실패 적중으로 정산한다`() {
        val user = guestUser()
        val agents = listOf(agent(), agent(), agent())
        `when`(agentRepository.findAllByUserId(requireNotNull(user.id))).thenReturn(agents)

        val day1 = LocalDate.of(2026, 9, 16)
        val day2 = LocalDate.of(2026, 9, 17)
        val day3 = LocalDate.of(2026, 9, 18)
        stubDay(day1, stockId = 1L, changeRate = BigDecimal("2.00"), priceId = 101L)
        stubDay(day2, stockId = 2L, changeRate = BigDecimal("-1.50"), priceId = 102L)
        stubDay(day3, stockId = 3L, changeRate = BigDecimal("0.00"), priceId = 103L)

        val decision1 = decisionMock(id = 201L)
        val decision2 = decisionMock(id = 202L)
        val decision3 = decisionMock(id = 203L)
        `when`(decisionRepository.save(any(Decision::class.java))).thenReturn(decision1, decision2, decision3)

        service.seed(user)

        val userPublicId = requireNotNull(user.publicId)
        verify(apTransactionService).change(
            userPublicId,
            -50_000,
            ApTransactionReason.DECISION_ENTRY_FEE,
            ApTransactionService.Target(ApTransactionTargetType.DECISION, 201L),
        )
        verify(apTransactionService).change(
            userPublicId,
            -50_000,
            ApTransactionReason.DECISION_ENTRY_FEE,
            ApTransactionService.Target(ApTransactionTargetType.DECISION, 202L),
        )
        verify(apTransactionService).change(
            userPublicId,
            -50_000,
            ApTransactionReason.DECISION_ENTRY_FEE,
            ApTransactionService.Target(ApTransactionTargetType.DECISION, 203L),
        )
        verify(badgeAwardService, times(3)).awardBadge(userPublicId, BadgeCode.B02)
        verify(decisionSettlementService).settle(DecisionSettlementItem(201L, 101L, true))
        verify(decisionSettlementService).settle(DecisionSettlementItem(202L, 102L, false))
        verify(decisionSettlementService).settle(DecisionSettlementItem(203L, 103L, true))
    }

    @Test
    fun `해당 날짜에 뉴스카드가 없으면 그 날짜는 건너뛴다`() {
        val user = guestUser()
        `when`(agentRepository.findAllByUserId(requireNotNull(user.id))).thenReturn(listOf(agent()))
        stubDay(LocalDate.of(2026, 9, 16), stockId = 1L, changeRate = BigDecimal("2.00"), priceId = 101L)
        `when`(newsCardRepository.findFirstByDisplayDateOrderByIdAsc(LocalDate.of(2026, 9, 17))).thenReturn(null)
        stubDay(LocalDate.of(2026, 9, 18), stockId = 3L, changeRate = BigDecimal("0.00"), priceId = 103L)
        val decision1 = decisionMock(id = 201L)
        val decision3 = decisionMock(id = 203L)
        `when`(decisionRepository.save(any(Decision::class.java))).thenReturn(decision1, decision3)

        service.seed(user)

        assertEquals(2, mockingDetails(decisionSettlementService).invocations.size)
    }

    @Test
    fun `해당 날짜에 종가가 없으면 그 날짜는 건너뛴다`() {
        val user = guestUser()
        `when`(agentRepository.findAllByUserId(requireNotNull(user.id))).thenReturn(listOf(agent()))
        stubDay(LocalDate.of(2026, 9, 16), stockId = 1L, changeRate = BigDecimal("2.00"), priceId = 101L)
        val day2NewsCard = newsCard(stockId = 2L, date = LocalDate.of(2026, 9, 17))
        `when`(newsCardRepository.findFirstByDisplayDateOrderByIdAsc(LocalDate.of(2026, 9, 17))).thenReturn(day2NewsCard)
        `when`(dailyStockPriceRepository.findByStockIdAndTradeDateAndIsClosingTrue(2L, LocalDate.of(2026, 9, 17)))
            .thenReturn(null)
        stubDay(LocalDate.of(2026, 9, 18), stockId = 3L, changeRate = BigDecimal("0.00"), priceId = 103L)
        val decision1 = decisionMock(id = 201L)
        val decision3 = decisionMock(id = 203L)
        `when`(decisionRepository.save(any(Decision::class.java))).thenReturn(decision1, decision3)

        service.seed(user)

        assertEquals(2, mockingDetails(decisionSettlementService).invocations.size)
    }

    @Test
    fun `에이전트가 없으면 아무것도 만들지 않는다`() {
        val user = guestUser()
        `when`(agentRepository.findAllByUserId(requireNotNull(user.id))).thenReturn(emptyList())

        service.seed(user)

        verifyNoInteractions(
            newsCardRepository,
            briefingRepository,
            decisionRepository,
            apTransactionService,
            badgeAwardService,
            decisionSettlementService,
        )
    }

    private fun stubDay(
        date: LocalDate,
        stockId: Long,
        changeRate: BigDecimal,
        priceId: Long,
    ) {
        val card = newsCard(stockId, date)
        `when`(newsCardRepository.findFirstByDisplayDateOrderByIdAsc(date)).thenReturn(card)
        val price = mock(DailyStockPrice::class.java)
        `when`(price.id).thenReturn(priceId)
        `when`(price.changeRate).thenReturn(changeRate)
        `when`(dailyStockPriceRepository.findByStockIdAndTradeDateAndIsClosingTrue(stockId, date)).thenReturn(price)
    }

    private fun newsCard(
        stockId: Long,
        date: LocalDate,
    ): NewsCard {
        val stock = mock(Stock::class.java)
        `when`(stock.id).thenReturn(stockId)
        `when`(stock.name).thenReturn("종목$stockId")
        val news = mock(News::class.java)
        `when`(news.stock).thenReturn(stock)
        val card = mock(NewsCard::class.java)
        `when`(card.news).thenReturn(news)
        `when`(card.displayDate).thenReturn(date)
        `when`(card.headline).thenReturn("헤드라인$stockId")
        `when`(card.points).thenReturn(listOf("포인트$stockId"))
        return card
    }

    private fun decisionMock(id: Long): Decision {
        val decision = mock(Decision::class.java)
        `when`(decision.id).thenReturn(id)
        return decision
    }

    private fun guestUser(): User =
        User.create(OAuthProvider.GUEST, "social-id", null).also {
            ReflectionTestUtils.setField(it, "id", 1L)
            ReflectionTestUtils.setField(it, "publicId", UUID.randomUUID())
        }

    private fun agent(): Agent = mock(Agent::class.java)
}
