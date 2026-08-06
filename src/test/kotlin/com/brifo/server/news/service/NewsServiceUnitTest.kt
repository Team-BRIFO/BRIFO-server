package com.brifo.server.news.service

import com.brifo.server.global.code.ErrorCode
import com.brifo.server.global.exception.BusinessException
import com.brifo.server.news.entity.News
import com.brifo.server.news.entity.NewsCard
import com.brifo.server.news.exception.NewsCardNotFoundException
import com.brifo.server.news.repository.NewsCardRepository
import com.brifo.server.news.repository.NewsDailyStockPriceRepository
import com.brifo.server.stock.entity.Stock
import com.brifo.server.term.repository.NewsCardTermRepository
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NewsServiceUnitTest {
    // DB 대신 사용할 가짜 Repository를 만든다.
    private val newsCardRepository = mock(NewsCardRepository::class.java)
    private val priceRepository = mock(NewsDailyStockPriceRepository::class.java)
    private val termRepository = mock(NewsCardTermRepository::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-07-04T03:00:00Z"), ZoneId.of("Asia/Seoul"))

    // 가짜 Repository를 주입해 Service만 테스트한다.
    private val newsService = NewsService(newsCardRepository, priceRepository, termRepository, clock)

    @Test
    fun `노출할 카드뉴스가 두 개가 아니면 예외가 발생한다`() {
        val stockId = UUID.randomUUID()

        `when`(newsCardRepository.findAnalysisCards(stockId, LocalDate.of(2026, 7, 4))).thenReturn(emptyList())

        assertFailsWith<NewsCardNotFoundException> {
            newsService.getNewsCards(stockId)
        }
    }

    @Test
    fun `가격이 없으면 예외가 발생한다`() {
        val stockId = UUID.randomUUID()
        val displayDate = LocalDate.of(2026, 7, 4)
        val firstNewsCard = mock(NewsCard::class.java)
        val secondNewsCard = mock(NewsCard::class.java)
        val news = mock(News::class.java)
        val stock = mock(Stock::class.java)

        `when`(newsCardRepository.findAnalysisCards(stockId, displayDate))
            .thenReturn(listOf(firstNewsCard, secondNewsCard))
        `when`(firstNewsCard.news).thenReturn(news)
        `when`(news.stock).thenReturn(stock)
        `when`(stock.id).thenReturn(2L)

        `when`(
            priceRepository.findTopByStockIdAndTradeDateLessThanEqualOrderByTradeDateDescFetchedAtDescIdDesc(
                2L,
                displayDate,
            ),
        ).thenReturn(null)

        val exception =
            assertFailsWith<BusinessException> {
                newsService.getNewsCards(stockId)
            }

        verify(priceRepository)
            .findTopByStockIdAndTradeDateLessThanEqualOrderByTradeDateDescFetchedAtDescIdDesc(
                2L,
                displayDate,
            )
        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR, exception.errorCode)
    }
}
