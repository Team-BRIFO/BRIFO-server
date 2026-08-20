package com.brifo.server.news.service

import com.brifo.server.global.exception.BusinessException
import com.brifo.server.news.entity.ImportanceBadge
import com.brifo.server.news.entity.News
import com.brifo.server.news.entity.NewsCard
import com.brifo.server.news.entity.NewsSource
import com.brifo.server.news.repository.NewsCardRepository
import com.brifo.server.stock.code.StockErrorCode
import com.brifo.server.stock.dto.response.PriceStatus
import com.brifo.server.stock.dto.response.StockPriceResult
import com.brifo.server.stock.entity.Stock
import com.brifo.server.stock.exception.StockNotFoundException
import com.brifo.server.stock.repository.StockRepository
import com.brifo.server.stock.service.StockPriceService
import com.brifo.server.term.repository.NewsCardTermRepository
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NewsServiceUnitTest {
    // DB 대신 사용할 가짜 Repository를 만든다.
    private val newsCardRepository = mock(NewsCardRepository::class.java)
    private val termRepository = mock(NewsCardTermRepository::class.java)
    private val stockRepository = mock(StockRepository::class.java)
    private val stockPriceService = mock(StockPriceService::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-07-04T03:00:00Z"), ZoneId.of("Asia/Seoul"))

    // 가짜 Repository를 주입해 Service만 테스트한다.
    private val newsService =
        NewsService(newsCardRepository, termRepository, stockRepository, stockPriceService, clock)

    @Test
    fun `노출할 카드뉴스가 없어도 종목 정보와 빈 목록을 응답한다`() {
        val stockId = UUID.randomUUID()
        val stock = mock(Stock::class.java)

        `when`(newsCardRepository.findDisplayCards(stockId, LocalDate.of(2026, 7, 4))).thenReturn(emptyList())
        `when`(stockRepository.findByPublicId(stockId)).thenReturn(stock)
        `when`(stock.id).thenReturn(2L)
        `when`(stock.code).thenReturn("005930")
        `when`(stock.publicId).thenReturn(stockId)
        `when`(stock.name).thenReturn("삼성전자")
        `when`(stock.sector).thenReturn("IT")
        `when`(stockPriceService.getCurrentPrice(2L, "005930")).thenReturn(
            StockPriceResult(
                stockCode = "005930",
                currentPrice = BigDecimal("70000"),
                priceChange = BigDecimal("850"),
                changeRate = BigDecimal("1.23"),
                priceStatus = PriceStatus.DELAYED_CURRENT,
                tradeDate = LocalDate.of(2026, 7, 4),
            ),
        )

        val response = newsService.getNewsCards(stockId)

        assertEquals(emptyList(), response.newsCards)
        assertEquals("삼성전자", response.stock.name)
    }

    @Test
    fun `그날 그 종목의 카드뉴스를 전부 응답한다`() {
        val stockId = UUID.randomUUID()
        val displayDate = LocalDate.of(2026, 7, 4)
        val stock = mock(Stock::class.java)
        val cards = List(5) { index -> newsCard(stock, cardId = UUID.randomUUID(), headline = "헤드라인 $index") }

        `when`(newsCardRepository.findDisplayCards(stockId, displayDate)).thenReturn(cards)
        `when`(termRepository.findAllByNewsCardIdOrderByDisplayOrderAsc(anyLong())).thenReturn(emptyList())
        `when`(stock.id).thenReturn(2L)
        `when`(stock.code).thenReturn("000660")
        `when`(stock.publicId).thenReturn(stockId)
        `when`(stock.name).thenReturn("SK하이닉스")
        `when`(stock.sector).thenReturn("IT")
        `when`(stockPriceService.getCurrentPrice(2L, "000660")).thenReturn(
            StockPriceResult(
                stockCode = "000660",
                currentPrice = BigDecimal("200000"),
                priceChange = BigDecimal("1000"),
                changeRate = BigDecimal("0.50"),
                priceStatus = PriceStatus.DELAYED_CURRENT,
                tradeDate = displayDate,
            ),
        )

        val response = newsService.getNewsCards(stockId)

        // 분석용 2장 제한(findAnalysisCards)이 상세 목록에 새어들지 않아야 한다.
        assertEquals(5, response.newsCards.size)
        assertEquals(cards.map { it.headline }, response.newsCards.map { it.headline })
    }

    @Test
    fun `카드뉴스도 종목도 없으면 예외가 발생한다`() {
        val stockId = UUID.randomUUID()

        `when`(newsCardRepository.findDisplayCards(stockId, LocalDate.of(2026, 7, 4))).thenReturn(emptyList())
        `when`(stockRepository.findByPublicId(stockId)).thenReturn(null)

        assertFailsWith<StockNotFoundException> {
            newsService.getNewsCards(stockId)
        }
    }

    @Test
    fun `현재가를 조회할 수 없으면 예외가 발생한다`() {
        val stockId = UUID.randomUUID()
        val displayDate = LocalDate.of(2026, 7, 4)
        val firstNewsCard = mock(NewsCard::class.java)
        val news = mock(News::class.java)
        val stock = mock(Stock::class.java)

        `when`(
            newsCardRepository.findDisplayCards(
                stockId,
                displayDate,
            ),
        ).thenReturn(
            listOf(firstNewsCard),
        )

        `when`(firstNewsCard.news).thenReturn(news)
        `when`(news.stock).thenReturn(stock)
        `when`(stock.id).thenReturn(2L)
        `when`(stock.code).thenReturn("BRIFO01")

        `when`(
            stockPriceService.getCurrentPrice(2L, "BRIFO01"),
        ).thenThrow(BusinessException(StockErrorCode.STOCK_PRICE_UNAVAILABLE))

        val exception =
            assertFailsWith<BusinessException> {
                newsService.getNewsCards(stockId)
            }

        verify(stockPriceService).getCurrentPrice(2L, "BRIFO01")

        assertEquals(
            StockErrorCode.STOCK_PRICE_UNAVAILABLE,
            exception.errorCode,
        )
    }

    private fun newsCard(
        stock: Stock,
        cardId: UUID,
        headline: String,
    ): NewsCard {
        val news = mock(News::class.java)
        val card = mock(NewsCard::class.java)
        `when`(news.stock).thenReturn(stock)
        `when`(news.source).thenReturn(NewsSource.NAVER)
        `when`(news.publishedAt).thenReturn(LocalDateTime.of(2026, 7, 4, 9, 0))
        `when`(card.news).thenReturn(news)
        `when`(card.importanceBadge).thenReturn(ImportanceBadge.MID)
        `when`(card.id).thenReturn(cardId.mostSignificantBits)
        `when`(card.publicId).thenReturn(cardId)
        `when`(card.headline).thenReturn(headline)
        `when`(card.points).thenReturn(emptyList())
        `when`(card.keywords).thenReturn(emptyList())
        return card
    }
}
