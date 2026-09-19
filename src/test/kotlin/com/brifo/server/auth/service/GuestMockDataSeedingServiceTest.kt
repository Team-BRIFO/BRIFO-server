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
import com.brifo.server.news.repository.NewsRepository
import com.brifo.server.stock.entity.DailyStockPrice
import com.brifo.server.stock.entity.Stock
import com.brifo.server.stock.entity.UserStock
import com.brifo.server.stock.repository.DailyStockPriceRepository
import com.brifo.server.stock.repository.UserStockRepository
import com.brifo.server.user.entity.OAuthProvider
import com.brifo.server.user.entity.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.test.util.ReflectionTestUtils
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

class GuestMockDataSeedingServiceTest {
    private val newsRepository = mock(NewsRepository::class.java)
    private val newsCardRepository = mock(NewsCardRepository::class.java)
    private val dailyStockPriceRepository = mock(DailyStockPriceRepository::class.java)
    private val agentRepository = mock(AgentRepository::class.java)
    private val userStockRepository = mock(UserStockRepository::class.java)
    private val briefingRepository = mock(BriefingRepository::class.java)
    private val decisionRepository = mock(DecisionRepository::class.java)
    private val apTransactionService = mock(ApTransactionService::class.java)
    private val badgeAwardService = mock(BadgeAwardService::class.java)
    private val decisionSettlementService = mock(DecisionSettlementService::class.java)
    private val calculator = DecisionSettlementCalculator()

    private lateinit var service: GuestMockDataSeedingService

    private val fixedDates =
        listOf(
            LocalDate.of(2026, 9, 18),
            LocalDate.of(2026, 9, 17),
            LocalDate.of(2026, 9, 16),
            LocalDate.of(2026, 9, 15),
            LocalDate.of(2026, 9, 14),
        )

    @BeforeEach
    fun setUp() {
        service =
            GuestMockDataSeedingService(
                newsRepository,
                newsCardRepository,
                dailyStockPriceRepository,
                agentRepository,
                userStockRepository,
                briefingRepository,
                decisionRepository,
                apTransactionService,
                badgeAwardService,
                decisionSettlementService,
                calculator,
            )
    }

    @Test
    fun `관심종목이 하나면 고정된 5일 모두에 실제 데이터로 하루 한 건씩 채운다`() {
        val user = guestUser()
        `when`(agentRepository.findAllByUserId(requireNotNull(user.id))).thenReturn(listOf(agent(), agent(), agent()))

        val stock = stock(stockId = 1L, name = "카카오")
        stubInterestStocks(user, stock)

        val changeRates = listOf(BigDecimal("2.00"), BigDecimal("-1.50"), BigDecimal("1.00"), BigDecimal("0.80"), BigDecimal("-2.00"))
        val priceIds = listOf(101L, 102L, 103L, 104L, 105L)
        fixedDates.forEachIndexed { i, date -> stubDay(date, stock, changeRates[i], priceIds[i]) }

        val decisions = priceIds.map { decisionMock(id = 200L + it) }
        `when`(decisionRepository.save(any(Decision::class.java))).thenReturn(decisions[0], *decisions.drop(1).toTypedArray())

        service.seed(user)

        val userPublicId = requireNotNull(user.publicId)
        // 5일 모두 결정일기가 생겼는지 — 각 날짜의 종가 id로 정산이 걸렸는지 확인한다.
        assertEquals(5, mockingDetails(decisionSettlementService).invocations.size)
        verify(decisionSettlementService).settle(DecisionSettlementItem(301L, 101L, true))
        verify(decisionSettlementService).settle(DecisionSettlementItem(302L, 102L, false))
        verify(decisionSettlementService).settle(DecisionSettlementItem(303L, 103L, true))
        verify(decisionSettlementService).settle(DecisionSettlementItem(304L, 104L, true))
        verify(decisionSettlementService).settle(DecisionSettlementItem(305L, 105L, false))
        verify(badgeAwardService, times(5)).awardBadge(userPublicId, BadgeCode.B02)

        // 실제 데이터가 있었으므로 안내용 뉴스카드·종가를 새로 만들 필요가 없다.
        verify(newsCardRepository, never()).save(any(NewsCard::class.java))
        verify(dailyStockPriceRepository, never()).save(any(DailyStockPrice::class.java))
    }

    @Test
    fun `관심종목이 여러 개면 하루에도 종목 수만큼 여러 건이 생기고 같은 날 같은 종목은 두 번 나오지 않는다`() {
        val user = guestUser()
        `when`(agentRepository.findAllByUserId(requireNotNull(user.id))).thenReturn(listOf(agent()))

        val stockA = stock(stockId = 1L, name = "현대차")
        val stockB = stock(stockId = 2L, name = "네이버")
        stubInterestStocks(user, stockA, stockB)
        // 실제 데이터는 스텁하지 않는다 — 안내용 데이터로 채워지는 경로에서 (날짜, 종목) 조합만 검증한다.
        `when`(newsRepository.save(any(News::class.java))).thenAnswer { it.arguments[0] }
        `when`(newsCardRepository.save(any(NewsCard::class.java))).thenAnswer { it.arguments[0] }
        val nextPriceId = AtomicLong(1L)
        `when`(dailyStockPriceRepository.save(any(DailyStockPrice::class.java))).thenAnswer { invocation ->
            val price = invocation.arguments[0] as DailyStockPrice
            ReflectionTestUtils.setField(price, "id", nextPriceId.getAndIncrement())
            price
        }
        `when`(decisionRepository.save(any(Decision::class.java))).thenAnswer { decisionMock(id = nextPriceId.get()) }

        service.seed(user)

        val dedupKeys = mockingDetails(newsRepository).invocations.map { (it.arguments[0] as News).dedupKey }
        // 날짜 5개 × 종목 2개 = 10개의 (날짜, 종목) 조합이 전부 서로 달라야 한다(중복 없음 = 같은 날 같은 종목 재사용 없음).
        assertEquals(10, dedupKeys.size)
        assertEquals(10, dedupKeys.distinct().size)
        assertEquals(5, dedupKeys.count { it.startsWith("guest-mock:1:") })
        assertEquals(5, dedupKeys.count { it.startsWith("guest-mock:2:") })
        assertEquals(10, mockingDetails(decisionSettlementService).invocations.size)
    }

    @Test
    fun `관심종목이 아닌 종목의 뉴스카드는 조회하지 않는다`() {
        val user = guestUser()
        `when`(agentRepository.findAllByUserId(requireNotNull(user.id))).thenReturn(listOf(agent()))

        val interestStock = stock(stockId = 1L, name = "현대차")
        val otherStock = stock(stockId = 99L, name = "삼성전자")
        stubInterestStocks(user, interestStock)
        val changeRates = listOf(BigDecimal("2.00"), BigDecimal("-1.50"), BigDecimal("1.20"), BigDecimal("0.90"), BigDecimal("-1.10"))
        fixedDates.forEachIndexed { i, date -> stubDay(date, interestStock, changeRates[i], 100L + i) }
        `when`(decisionRepository.save(any(Decision::class.java))).thenAnswer { decisionMock(id = System.nanoTime()) }

        service.seed(user)

        // Mockito의 eq()/any()는 항상 null을 반환하므로, findDailyCards의 non-null UUID 파라미터에
        // matcher를 넣으면 Kotlin의 null 체크에 걸려 NPE가 난다. 그래서 실제 호출 인자를 직접 검사한다.
        val otherStockPublicId = requireNotNull(otherStock.publicId)
        val queriedStockPublicIds = mockingDetails(newsCardRepository).invocations.map { it.arguments[0] }
        assertFalse(queriedStockPublicIds.contains(otherStockPublicId))
    }

    @Test
    fun `관심종목에 실제 뉴스카드·종가가 없으면 안내용 데이터를 만들어서라도 5일 모두 채운다`() {
        val user = guestUser()
        `when`(agentRepository.findAllByUserId(requireNotNull(user.id))).thenReturn(listOf(agent()))

        val stock = stock(stockId = 1L, name = "이름없는종목")
        stubInterestStocks(user, stock)
        // 뉴스카드·종가 조회를 스텁하지 않는다 = 항상 못 찾는다(Mockito 기본값: 빈 리스트/null).

        `when`(newsRepository.save(any(News::class.java))).thenAnswer { it.arguments[0] }
        `when`(newsCardRepository.save(any(NewsCard::class.java))).thenAnswer { it.arguments[0] }
        val nextPriceId = AtomicLong(901L)
        `when`(dailyStockPriceRepository.save(any(DailyStockPrice::class.java))).thenAnswer { invocation ->
            val price = invocation.arguments[0] as DailyStockPrice
            ReflectionTestUtils.setField(price, "id", nextPriceId.getAndIncrement())
            price
        }
        val decisions = (301L..305L).map { decisionMock(id = it) }
        `when`(decisionRepository.save(any(Decision::class.java))).thenReturn(decisions[0], *decisions.drop(1).toTypedArray())

        service.seed(user)

        assertEquals(5, mockingDetails(decisionSettlementService).invocations.size)
        verify(newsRepository, times(5)).save(any(News::class.java))
        verify(newsCardRepository, times(5)).save(any(NewsCard::class.java))
        verify(dailyStockPriceRepository, times(5)).save(any(DailyStockPrice::class.java))
        verify(decisionSettlementService).settle(DecisionSettlementItem(301L, 901L, true))
        verify(decisionSettlementService).settle(DecisionSettlementItem(302L, 902L, false))
        verify(decisionSettlementService).settle(DecisionSettlementItem(303L, 903L, true))
        verify(decisionSettlementService).settle(DecisionSettlementItem(304L, 904L, true))
        verify(decisionSettlementService).settle(DecisionSettlementItem(305L, 905L, false))
    }

    @Test
    fun `에이전트가 없으면 아무것도 만들지 않는다`() {
        val user = guestUser()
        `when`(agentRepository.findAllByUserId(requireNotNull(user.id))).thenReturn(emptyList())

        service.seed(user)

        verifyNoInteractions(
            userStockRepository,
            newsRepository,
            newsCardRepository,
            briefingRepository,
            decisionRepository,
            apTransactionService,
            badgeAwardService,
            decisionSettlementService,
        )
    }

    @Test
    fun `관심종목이 없으면 아무것도 만들지 않는다`() {
        val user = guestUser()
        `when`(agentRepository.findAllByUserId(requireNotNull(user.id))).thenReturn(listOf(agent()))
        `when`(userStockRepository.findAllByUser(user)).thenReturn(emptyList())

        service.seed(user)

        verifyNoInteractions(
            newsRepository,
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
        stock: Stock,
        changeRate: BigDecimal,
        priceId: Long,
    ) {
        val card = newsCard(stock)
        `when`(newsCardRepository.findDailyCards(requireNotNull(stock.publicId), date)).thenReturn(listOf(card))
        val price = mock(DailyStockPrice::class.java)
        `when`(price.id).thenReturn(priceId)
        `when`(price.changeRate).thenReturn(changeRate)
        `when`(
            dailyStockPriceRepository.findByStockIdAndTradeDateAndIsClosingTrue(requireNotNull(stock.id), date),
        ).thenReturn(price)
    }

    private fun newsCard(stock: Stock): NewsCard {
        val stockId = stock.id
        val news = mock(News::class.java)
        `when`(news.stock).thenReturn(stock)
        val card = mock(NewsCard::class.java)
        `when`(card.news).thenReturn(news)
        `when`(card.headline).thenReturn("헤드라인$stockId")
        `when`(card.points).thenReturn(listOf("포인트$stockId"))
        return card
    }

    private fun stock(
        stockId: Long,
        name: String,
    ): Stock {
        val stock = mock(Stock::class.java)
        `when`(stock.id).thenReturn(stockId)
        `when`(stock.publicId).thenReturn(UUID.randomUUID())
        `when`(stock.name).thenReturn(name)
        return stock
    }

    /** 관심종목 mock을 미리 만들어둔 뒤 스터빙한다 — thenReturn() 인자 안에서 새로 when()을 걸면 Mockito가 이전 스터빙을 미완료로 본다. */
    private fun stubInterestStocks(
        user: User,
        vararg stocks: Stock,
    ) {
        val userStocks =
            stocks.map { stock ->
                val userStock = mock(UserStock::class.java)
                `when`(userStock.user).thenReturn(user)
                `when`(userStock.stock).thenReturn(stock)
                userStock
            }
        `when`(userStockRepository.findAllByUser(user)).thenReturn(userStocks)
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
