package com.brifo.server.batch.dev

import com.brifo.server.auth.dev.DevAuthProperties
import com.brifo.server.batch.generation.NewsCardPersistenceService
import com.brifo.server.news.client.NewsCardGenerationClient
import com.brifo.server.news.entity.News
import com.brifo.server.news.entity.NewsCard
import com.brifo.server.news.repository.NewsCardRepository
import com.brifo.server.news.repository.NewsRepository
import com.brifo.server.stock.entity.Stock
import com.brifo.server.term.repository.NewsCardTermRepository
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.launch.JobOperator
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals

class DevBatchServiceTest {
    private val cleanupService = mock(DevBatchCleanupService::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-08-10T03:00:00Z"), ZoneId.of("Asia/Seoul"))
    private val newsRepository = mock(NewsRepository::class.java)
    private val newsCardRepository = mock(NewsCardRepository::class.java)
    private val newsCardTermRepository = mock(NewsCardTermRepository::class.java)
    private val generationClient = mock(NewsCardGenerationClient::class.java)
    private val persistenceService = mock(NewsCardPersistenceService::class.java)
    private val jobOperator = mock(JobOperator::class.java)
    private val newsCollectionJob = mock(Job::class.java)
    private val newsCardGenerationJob = mock(Job::class.java)
    private val decisionSettlementJob = mock(Job::class.java)
    private val service = DevBatchService(
        properties = DevAuthProperties(enabled = true, password = "secret"),
        cleanupService = cleanupService,
        clock = clock,
        newsRepository = newsRepository,
        newsCardRepository = newsCardRepository,
        newsCardTermRepository = newsCardTermRepository,
        newsCardGenerationClient = generationClient,
        newsCardPersistenceService = persistenceService,
        jobOperator = jobOperator,
        newsCollectionJob = newsCollectionJob,
        newsCardGenerationJob = newsCardGenerationJob,
        decisionSettlementJob = decisionSettlementJob,
    )

    @Test
    fun `단일 카드뉴스 생성은 기존 생성 로직으로 AI 서버를 호출하고 저장한다`() {
        val newsPublicId = UUID.randomUUID()
        val cardPublicId = UUID.randomUUID()
        val publishedAt = LocalDateTime.of(2026, 8, 10, 15, 0)
        val displayDate = LocalDate.of(2026, 8, 10)
        val news = mock(News::class.java)
        val stock = mock(Stock::class.java)
        val newsCard = mock(NewsCard::class.java)
        val aiCard = NewsCardGenerationClient.CardNews(
            headline = "생성된 카드",
            points = listOf("핵심 내용"),
            keywords = listOf("키워드"),
        )

        `when`(news.id).thenReturn(1L)
        `when`(news.publicId).thenReturn(newsPublicId)
        `when`(news.publishedAt).thenReturn(publishedAt)
        `when`(news.summary).thenReturn("뉴스 요약")
        `when`(news.stock).thenReturn(stock)
        `when`(stock.name).thenReturn("테스트 종목")
        `when`(newsRepository.findByPublicId(newsPublicId)).thenReturn(news)
        `when`(newsRepository.findById(1L)).thenReturn(Optional.of(news))
        `when`(newsCardTermRepository.findTermsUsedBetween(LocalDate.of(2026, 7, 28), LocalDate.of(2026, 8, 11)))
            .thenReturn(emptyList())
        `when`(
            generationClient.createNewsCard(
                NewsCardGenerationClient.Request(
                    newsId = newsPublicId,
                    stockName = "테스트 종목",
                    newsContent = "뉴스 요약",
                    excludeTerms = emptyList(),
                ),
            ),
        ).thenReturn(NewsCardGenerationClient.Result(newsPublicId, listOf(aiCard)))
        `when`(newsCard.publicId).thenReturn(cardPublicId)
        `when`(newsCardRepository.findByNewsId(1L)).thenReturn(newsCard)

        val response = service.runSingleNewsCardGeneration(
            DevSingleNewsCardGenerationRequest(password = "secret", newsId = newsPublicId),
        )

        verify(cleanupService).cleanupForSingleGeneration(1L)
        verify(generationClient).createNewsCard(
            NewsCardGenerationClient.Request(
                newsId = newsPublicId,
                stockName = "테스트 종목",
                newsContent = "뉴스 요약",
                excludeTerms = emptyList(),
            ),
        )
        verify(persistenceService).save(
            com.brifo.server.batch.generation.GeneratedNewsCardItem(1L, aiCard),
            displayDate,
        )
        assertEquals(newsPublicId, response.newsId)
        assertEquals(cardPublicId, response.newsCardId)
        assertEquals(displayDate, response.displayDate)
    }
}
