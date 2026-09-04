package com.brifo.server.news.controller

import com.brifo.server.ServerTestConfiguration
import com.brifo.server.news.entity.ImportanceBadge
import com.brifo.server.news.entity.News
import com.brifo.server.news.entity.NewsCard
import com.brifo.server.news.entity.NewsSource
import com.brifo.server.stock.dto.response.PriceStatus
import com.brifo.server.stock.dto.response.StockPriceResult
import com.brifo.server.stock.entity.DailyStockPrice
import com.brifo.server.stock.entity.Stock
import com.brifo.server.stock.service.StockPriceService
import com.brifo.server.term.entity.GlossaryTerm
import com.brifo.server.term.entity.NewsCardTerm
import jakarta.persistence.EntityManager
import org.hamcrest.Matchers.hasItem
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@Import(ServerTestConfiguration::class)
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class NewsControllerIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val entityManager: EntityManager,
    private val clock: Clock,
) {
    @MockitoBean
    private lateinit var stockPriceService: StockPriceService

    @Test
    fun `종목의 오늘 카드뉴스 두 개를 조회한다`() {
        val stockId = saveNewsCardData()
        val displayDate = LocalDate.now(clock)

        mockMvc
            .perform(
                get("/api/stocks/{stockId}/news-cards", stockId)
                    .param("userId", UUID.randomUUID().toString()),
            )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("COMMON_200"))
            .andExpect(jsonPath("$.result.stock.stockId").value(stockId.toString()))
            .andExpect(jsonPath("$.result.stock.logoUrl").value("https://cdn.example.com/005930.png"))
            .andExpect(jsonPath("$.result.stock.tradeDate").value(displayDate.minusDays(1).toString()))
            .andExpect(jsonPath("$.result.stock.changeRate").value(2.1))
            .andExpect(jsonPath("$.result.newsCards").isArray)
            .andExpect(jsonPath("$.result.newsCards.length()").value(2))
            // 카드뉴스는 최신순(newsCard.id DESC)으로 내려간다.
            .andExpect(jsonPath("$.result.newsCards[0].headline").value("삼성전자 공급계약"))
            .andExpect(
                jsonPath("$.result.newsCards[0].publishedDate")
                    .value(displayDate.atTime(11, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant().toString()),
            )
            .andExpect(jsonPath("$.result.newsCards[1].headline").value("삼성전자 실적 개선"))
            .andExpect(
                jsonPath("$.result.newsCards[1].publishedDate")
                    .value(displayDate.atTime(10, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant().toString()),
            )
            .andExpect(jsonPath("$.result.newsCards[1].imageUrl").value("https://cdn.example.com/news/1.png"))
            .andExpect(jsonPath("$.result.newsCards[1].terms[0].displayOrder").value(0))
            .andExpect(jsonPath("$.result.newsCards[1].terms[1].displayOrder").value(1))
            .andExpect(jsonPath("$.result.newsCards[1].terms[1].surface").value("순매수"))
    }

    @Test
    fun `잘못된 요청은 오류를 반환한다`() {
        val userId = UUID.randomUUID().toString()

        mockMvc
            .perform(get("/api/stocks/not-a-uuid/news-cards").param("userId", userId))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("COMMON_400"))

        // 존재하지 않는 종목은 STOCK_404 다. 종목은 있는데 오늘 카드가 없는 경우는
        // 오류가 아니라 200 + 빈 목록으로 응답한다.
        mockMvc
            .perform(
                get("/api/stocks/{stockId}/news-cards", UUID.randomUUID())
                    .param("userId", userId),
            )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("STOCK_404"))
    }

    @Test
    fun `오늘 카드뉴스가 세 개 이상이면 전부 조회한다`() {
        val stockId = saveNewsCardData(includeExtraCard = true)

        // 분석용 2장 제한(findAnalysisCards)이 상세 목록에 새어들면 안 된다.
        mockMvc
            .perform(get("/api/stocks/{stockId}/news-cards", stockId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.result.newsCards.length()").value(3))
            .andExpect(jsonPath("$.result.newsCards[*].headline").value(hasItem("삼성전자 추가 뉴스")))
    }

    private fun saveNewsCardData(includeExtraCard: Boolean = false): UUID {
        val displayDate = LocalDate.now(clock)
        `when`(stockPriceService.getCurrentPrice(anyLong(), anyString())).thenReturn(
            StockPriceResult(
                stockCode = "005930",
                currentPrice = BigDecimal("79200"),
                priceChange = null,
                changeRate = BigDecimal("2.14"),
                priceStatus = PriceStatus.PREVIOUS_CLOSE,
                tradeDate = displayDate.minusDays(1),
            ),
        )
        val stock = Stock.create(
            code = "005930",
            name = "삼성전자",
            sector = "반도체",
            logoUrl = "https://cdn.example.com/005930.png",
        )
        entityManager.persist(stock)

        val firstNews =
            News.create(
                stock = stock,
                source = NewsSource.NAVER,
                sourceUrl = "https://example.com/news/1",
                title = "삼성전자 실적 개선",
                summary = null,
                importance = BigDecimal("0.90"),
                dedupKey = "news-card-test-1",
                publishedAt = displayDate.atTime(10, 0),
            )
        entityManager.persist(firstNews)

        val firstNewsCard =
            NewsCard.create(
                news = firstNews,
                headline = "삼성전자 실적 개선",
                points = listOf("메모리 수요 회복"),
                keywords = listOf("HBM3E"),
                importanceBadge = ImportanceBadge.HOT,
                displayDate = displayDate,
                imageUrl = "https://cdn.example.com/news/1.png",
            )
        entityManager.persist(firstNewsCard)

        val secondNews =
            News.create(
                stock = stock,
                source = NewsSource.DART,
                sourceUrl = "https://example.com/news/2",
                title = "삼성전자 공급계약",
                summary = null,
                importance = BigDecimal("0.80"),
                dedupKey = "news-card-test-2",
                publishedAt = displayDate.atTime(11, 0),
            )
        entityManager.persist(secondNews)
        entityManager.persist(
            NewsCard.create(
                news = secondNews,
                headline = "삼성전자 공급계약",
                points = listOf("신규 공급계약 체결"),
                keywords = listOf("공급계약"),
                importanceBadge = ImportanceBadge.MID,
                displayDate = displayDate,
            ),
        )

        if (includeExtraCard) {
            val thirdNews =
                News.create(
                    stock = stock,
                    source = NewsSource.KRX,
                    sourceUrl = "https://example.com/news/3",
                    title = "삼성전자 추가 뉴스",
                    summary = null,
                    importance = BigDecimal("0.70"),
                    dedupKey = "news-card-test-3",
                    publishedAt = displayDate.atTime(12, 0),
                )
            entityManager.persist(thirdNews)
            entityManager.persist(
                NewsCard.create(
                    news = thirdNews,
                    headline = "삼성전자 추가 뉴스",
                    points = listOf("추가 뉴스 포인트"),
                    keywords = listOf("추가"),
                    importanceBadge = ImportanceBadge.MID,
                    displayDate = displayDate,
                ),
            )
        }

        val prices =
            listOf(
                DailyStockPrice.create(stock, displayDate.minusDays(3), BigDecimal("78000"), BigDecimal("1.0")),
                DailyStockPrice.create(stock, displayDate.minusDays(1), BigDecimal("79200"), BigDecimal("2.14")),
                DailyStockPrice.create(stock, displayDate.plusDays(1), BigDecimal("80000"), BigDecimal("3.0")),
            )

        prices.forEach { price ->
            entityManager.persist(price)
        }

        // 카드뉴스에 연결할 용어를 저장한다.
        val firstTerm = GlossaryTerm.create("목표주가", "목표로 제시한 주가", "투자")
        val secondTerm = GlossaryTerm.create("순매수", "매수가 많은 상태", "수급")
        entityManager.persist(firstTerm)
        entityManager.persist(secondTerm)

        val secondCardTerm =
            NewsCardTerm.create(
                newsCard = firstNewsCard,
                term = secondTerm,
                surface = null,
                displayOrder = 1,
            )

        val firstCardTerm =
            NewsCardTerm.create(
                newsCard = firstNewsCard,
                term = firstTerm,
                surface = "목표주가",
                displayOrder = 0,
            )

        // 용어가 저장 순서와 상관없이 화면 순서대로 나오는지 확인한다.
        entityManager.persist(secondCardTerm)
        entityManager.persist(firstCardTerm)

        entityManager.flush()

        return requireNotNull(stock.publicId)
    }
}
