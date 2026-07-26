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
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NewsServiceUnitTest {
    // DB 대신 사용할 가짜 Repository를 만든다.
    private val newsCardRepository = mock(NewsCardRepository::class.java)
    private val priceRepository = mock(NewsDailyStockPriceRepository::class.java)
    private val termRepository = mock(NewsCardTermRepository::class.java)

    // 가짜 Repository를 주입해 Service만 테스트한다.
    private val newsService = NewsService(newsCardRepository, priceRepository, termRepository)

    @Test
    fun `카드뉴스가 없으면 예외가 발생한다`() {
        val cardId = UUID.randomUUID()

        // 해당 cardId의 카드뉴스가 없는 상황을 만든다.
        `when`(newsCardRepository.findByPublicId(cardId)).thenReturn(null)

        // 카드뉴스가 없으면 조회 실패 예외가 발생해야 한다.
        assertFailsWith<NewsCardNotFoundException> {
            newsService.getNewsCard(cardId)
        }
    }

    @Test
    fun `가격이 없으면 예외가 발생한다`() {
        val cardId = UUID.randomUUID()
        val publishedDate = LocalDate.of(2026, 7, 4)
        val newsCard = mock(NewsCard::class.java)
        val news = mock(News::class.java)
        val stock = mock(Stock::class.java)

        // 가격 조회까지 진행할 수 있도록 카드뉴스, 뉴스, 종목을 연결한다.
        `when`(newsCardRepository.findByPublicId(cardId)).thenReturn(newsCard)
        `when`(newsCard.news).thenReturn(news)
        `when`(news.stock).thenReturn(stock)

        // 가격 조회에 필요한 뉴스 발행일과 종목 ID를 설정한다.
        `when`(news.publishedAt).thenReturn(LocalDateTime.of(2026, 7, 4, 10, 0))
        `when`(stock.id).thenReturn(2L)

        // 뉴스 발행일 이전의 가격이 없는 상황을 만든다.
        `when`(
            priceRepository.findTopByStockIdAndTradeDateLessThanEqualOrderByTradeDateDesc(
                2L,
                publishedDate,
            ),
        ).thenReturn(null)

        // 필수 가격이 없으면 응답을 만들 수 없어야 한다.
        val exception =
            assertFailsWith<BusinessException> {
                newsService.getNewsCard(cardId)
            }

        verify(priceRepository)
            .findTopByStockIdAndTradeDateLessThanEqualOrderByTradeDateDesc(
                2L,
                publishedDate,
            )
        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR, exception.errorCode)
    }
}
