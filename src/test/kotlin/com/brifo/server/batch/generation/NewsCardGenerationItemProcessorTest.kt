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
                newsContent = "제목\n요약",
                excludeTerms = listOf("PER"),
            )
        `when`(client.createNewsCard(expectedRequest)).thenReturn(generated)

        val result = NewsCardGenerationItemProcessor(newsRepository, termRepository, client).process(3L)

        val request = mockingDetails(client).invocations.single { it.method.name == "createNewsCard" }.arguments[0]
            as NewsCardGenerationClient.Request
        assertEquals("브리포주식", request.stockName)
        assertEquals(listOf("PER"), request.excludeTerms)
        // 검색 스니펫이 기사 하단 정보로 잡히는 기사가 있어, 제목을 버리면 카드가 그 조각만 보고 생성된다.
        assertEquals("제목\n요약", request.newsContent)
        assertEquals(3L, result.newsId)
        assertEquals(card, result.card)
    }

    @Test
    fun `숫자만 다르고 나머지 문장이 같은 포인트는 먼저 나온 것만 남긴다`() {
        val newsRepository = mock(NewsRepository::class.java)
        val termRepository = mock(NewsCardTermRepository::class.java)
        val client = mock(NewsCardGenerationClient::class.java)
        val news = mock(News::class.java)
        val stock = mock(Stock::class.java)
        val newsPublicId = UUID.randomUUID()
        val card = NewsCardGenerationClient.CardNews(
            "LG전자, B2B 체질변화",
            listOf(
                "LG전자가 올해 1분기 기업간거래(B2B)에서 6조5000억원의 매출을 올렸다.",
                "LG전자가 올해 2분기 기업간거래(B2B)에서 6조5000억원의 매출을 올렸다.",
                "LG이노텍 실적을 제외한 전사 매출에서 B2B가 차지하는 비중이 다뤄졌다.",
            ),
            listOf("매출", "기업간거래"),
        )
        `when`(news.publicId).thenReturn(newsPublicId)
        `when`(news.stock).thenReturn(stock)
        `when`(stock.name).thenReturn("LG전자")
        `when`(news.title).thenReturn("제목")
        `when`(news.summary).thenReturn("요약")
        `when`(news.publishedAt).thenReturn(LocalDateTime.of(2026, 8, 3, 11, 0))
        `when`(newsRepository.findById(3L)).thenReturn(Optional.of(news))
        `when`(
            client.createNewsCard(
                NewsCardGenerationClient.Request(
                    newsId = newsPublicId,
                    stockName = "LG전자",
                    newsContent = "제목\n요약",
                    excludeTerms = emptyList(),
                ),
            ),
        ).thenReturn(NewsCardGenerationClient.Result(newsPublicId, listOf(card)))

        val result = NewsCardGenerationItemProcessor(newsRepository, termRepository, client).process(3L)

        assertEquals(
            listOf(
                "LG전자가 올해 1분기 기업간거래(B2B)에서 6조5000억원의 매출을 올렸다.",
                "LG이노텍 실적을 제외한 전사 매출에서 B2B가 차지하는 비중이 다뤄졌다.",
            ),
            result.card.points,
        )
    }

    @Test
    fun `내용이 겹치지 않는 포인트는 모두 남긴다`() {
        val newsRepository = mock(NewsRepository::class.java)
        val termRepository = mock(NewsCardTermRepository::class.java)
        val client = mock(NewsCardGenerationClient::class.java)
        val news = mock(News::class.java)
        val stock = mock(Stock::class.java)
        val newsPublicId = UUID.randomUUID()
        val points = listOf("첫 번째 포인트 문장입니다.", "완전히 다른 두 번째 문장입니다.", "세 번째도 서로 다릅니다.")
        val card = NewsCardGenerationClient.CardNews("헤드라인", points, listOf("키워드"))
        `when`(news.publicId).thenReturn(newsPublicId)
        `when`(news.stock).thenReturn(stock)
        `when`(stock.name).thenReturn("브리포주식")
        `when`(news.title).thenReturn("제목")
        `when`(news.summary).thenReturn("요약")
        `when`(news.publishedAt).thenReturn(LocalDateTime.of(2026, 8, 3, 11, 0))
        `when`(newsRepository.findById(3L)).thenReturn(Optional.of(news))
        `when`(
            client.createNewsCard(
                NewsCardGenerationClient.Request(
                    newsId = newsPublicId,
                    stockName = "브리포주식",
                    newsContent = "제목\n요약",
                    excludeTerms = emptyList(),
                ),
            ),
        ).thenReturn(NewsCardGenerationClient.Result(newsPublicId, listOf(card)))

        val result = NewsCardGenerationItemProcessor(newsRepository, termRepository, client).process(3L)

        assertEquals(points, result.card.points)
    }

    @Test
    fun `요약이 없거나 비어 있으면 제목만 카드 생성 본문으로 넘긴다`() {
        listOf(null, "  ").forEach { summary ->
            val newsRepository = mock(NewsRepository::class.java)
            val termRepository = mock(NewsCardTermRepository::class.java)
            val client = mock(NewsCardGenerationClient::class.java)
            val news = mock(News::class.java)
            val stock = mock(Stock::class.java)
            val newsPublicId = UUID.randomUUID()
            `when`(news.publicId).thenReturn(newsPublicId)
            `when`(news.stock).thenReturn(stock)
            `when`(stock.name).thenReturn("브리포주식")
            `when`(news.title).thenReturn("제목")
            `when`(news.summary).thenReturn(summary)
            `when`(news.publishedAt).thenReturn(LocalDateTime.of(2026, 8, 3, 11, 0))
            `when`(newsRepository.findById(3L)).thenReturn(Optional.of(news))
            `when`(
                client.createNewsCard(
                    NewsCardGenerationClient.Request(
                        newsId = newsPublicId,
                        stockName = "브리포주식",
                        newsContent = "제목",
                        excludeTerms = emptyList(),
                    ),
                ),
            ).thenReturn(
                NewsCardGenerationClient.Result(
                    newsPublicId,
                    listOf(NewsCardGenerationClient.CardNews("헤드라인", listOf("포인트"), listOf("키워드"))),
                ),
            )

            NewsCardGenerationItemProcessor(newsRepository, termRepository, client).process(3L)

            val request = mockingDetails(client).invocations.single { it.method.name == "createNewsCard" }.arguments[0]
                as NewsCardGenerationClient.Request
            assertEquals("제목", request.newsContent)
        }
    }
}
