package com.brifo.server.batch.collection

import com.brifo.server.batch.captureKotlin
import com.brifo.server.news.client.NewsCollectionClient
import com.brifo.server.news.client.DisclosureClient
import com.brifo.server.news.entity.News
import com.brifo.server.news.entity.NewsSource
import com.brifo.server.news.repository.NewsRepository
import com.brifo.server.stock.entity.Stock
import com.brifo.server.stock.repository.StockRepository
import org.junit.jupiter.api.Test
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.StepContribution
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals

class NewsCollectionTaskletTest {
    private val client = mock(NewsCollectionClient::class.java)
    private val disclosureClient = mock(DisclosureClient::class.java)
    private val stockRepository = mock(StockRepository::class.java)
    private val newsRepository = mock(NewsRepository::class.java)
    private val targetDate = LocalDate.of(2026, 8, 3)

    @Test
    fun `회차 기준 시각 이하의 신규 뉴스만 중요도를 계산해 저장한다`() {
        val stock = Stock.create("BRF001", "브리포주식", "금융")
        val existing = news("existing", LocalDateTime.of(2026, 8, 3, 7, 0))
        val candidate = news("candidate", LocalDateTime.of(2026, 8, 3, 11, 0))
        val tooLate = news("late", LocalDateTime.of(2026, 8, 3, 12, 0))
        `when`(stockRepository.findAllByIsActiveTrueOrderByCode()).thenReturn(listOf(stock))
        `when`(client.collect(NewsCollectionClient.Request(targetDate, targetDate.atTime(11, 30), listOf("BRF001"))))
            .thenReturn(NewsCollectionClient.Result(listOf(existing, candidate, tooLate)))
        `when`(newsRepository.existsByDedupKey("existing")).thenReturn(true)
        `when`(newsRepository.existsByDedupKey("candidate")).thenReturn(false)
        `when`(stockRepository.findByCode("BRF001")).thenReturn(stock)
        `when`(disclosureClient.exists(DisclosureClient.Request(null, "BRF001", targetDate))).thenReturn(true)

        tasklet(CollectionRound.MIDDAY).execute(
            mock(StepContribution::class.java),
            mock(ChunkContext::class.java),
        )

        val captor = ArgumentCaptor.forClass(News::class.java)
        verify(newsRepository).save(captureKotlin(captor))
        assertEquals("candidate", captor.value.dedupKey)
        assertEquals(java.math.BigDecimal("0.85"), captor.value.importance)
    }

    @Test
    fun `조회 결과가 없으면 정상 종료하고 저장하지 않는다`() {
        `when`(stockRepository.findAllByIsActiveTrueOrderByCode()).thenReturn(emptyList())
        `when`(client.collect(NewsCollectionClient.Request(targetDate, targetDate.atTime(7, 0), emptyList())))
            .thenReturn(NewsCollectionClient.Result(emptyList()))

        tasklet(CollectionRound.MORNING).execute(
            mock(StepContribution::class.java),
            mock(ChunkContext::class.java),
        )

        verify(newsRepository, never()).save(any(News::class.java))
    }

    private fun tasklet(round: CollectionRound) =
        NewsCollectionTasklet(
            targetDate,
            round,
            client,
            disclosureClient,
            stockRepository,
            newsRepository,
            NewsImportanceCalculator(),
        )

    private fun news(
        key: String,
        publishedAt: LocalDateTime,
    ) = NewsCollectionClient.CollectedNews(
        stockCode = "BRF001",
        source = NewsSource.TEST,
        sourceUrl = "https://example.com/$key",
        title = key,
        summary = "$key summary",
        dedupKey = key,
        publishedAt = publishedAt,
    )
}
