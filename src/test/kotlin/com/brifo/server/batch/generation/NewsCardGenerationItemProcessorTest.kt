package com.brifo.server.batch.generation

import com.brifo.server.news.client.NewsCardGenerationClient
import com.brifo.server.news.entity.News
import com.brifo.server.news.repository.NewsRepository
import com.brifo.server.stock.entity.Stock
import com.brifo.server.term.repository.NewsCardTermRepository
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals

class NewsCardGenerationItemProcessorTest {
    @Test
    fun `뉴스 한 건을 카드 생성 API 요청으로 변환한다`() {
        val newsRepository = mock(NewsRepository::class.java)
        val termRepository = mock(NewsCardTermRepository::class.java)
        val client = mock(NewsCardGenerationClient::class.java)
        val news = mock(News::class.java)
        val stock = mock(Stock::class.java)
        val newsPublicId = UUID.randomUUID()
        val card = NewsCardGenerationClient.CardNews("헤드라인", listOf("포인트"), listOf("키워드"))
        val generated = NewsCardGenerationClient.Result(newsPublicId, listOf(card))
        `when`(news.publicId).thenReturn(newsPublicId)
        `when`(news.stock).thenReturn(stock)
        `when`(stock.name).thenReturn("브리포주식")
        `when`(news.title).thenReturn("제목")
        `when`(news.summary).thenReturn("요약")
        `when`(news.publishedAt).thenReturn(LocalDateTime.of(2026, 8, 3, 11, 0))
        `when`(newsRepository.findById(3L)).thenReturn(Optional.of(news))
        `when`(
            termRepository.findTermsUsedBetween(
                LocalDateTime.of(2026, 7, 21, 0, 0).toLocalDate(),
                LocalDateTime.of(2026, 8, 4, 0, 0).toLocalDate(),
            ),
        ).thenReturn(listOf("PER"))
        val expectedRequest =
            NewsCardGenerationClient.Request(
                newsId = newsPublicId,
                stockName = "브리포주식",
                newsContent = "요약",
                excludeTerms = listOf("PER"),
            )
        `when`(client.createNewsCard(expectedRequest)).thenReturn(generated)

        val result = NewsCardGenerationItemProcessor(newsRepository, termRepository, client).process(3L)

        val request = mockingDetails(client).invocations.single { it.method.name == "createNewsCard" }.arguments[0]
            as NewsCardGenerationClient.Request
        assertEquals("브리포주식", request.stockName)
        assertEquals(listOf("PER"), request.excludeTerms)
        assertEquals(3L, result.newsId)
        assertEquals(card, result.card)
    }
}
