package com.brifo.server.batch.collection

import com.brifo.server.batch.captureKotlin
import com.brifo.server.news.client.NewsCollectionClient
import com.brifo.server.news.entity.News
import com.brifo.server.news.entity.NewsSource
import com.brifo.server.news.repository.NewsRepository
import com.brifo.server.stock.entity.Stock
import com.brifo.server.stock.repository.StockRepository
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.StepContribution
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals

class NewsCollectionTaskletTest {
    private val client = mock(NewsCollectionClient::class.java)
    private val stockRepository = mock(StockRepository::class.java)
    private val newsRepository = mock(NewsRepository::class.java)
    private val targetDate = LocalDate.of(2026, 8, 3)
    private val defaultWatchlistCodes = listOf("005930")

    @Test
    fun `중복되지 않은 뉴스만 발행 시각 기준 중요도를 계산해 저장한다`() {
        val stock = Stock.create("BRF001", "브리포주식", "금융")
        val existing = news("existing", LocalDateTime.of(2026, 8, 3, 7, 0))
        val candidate = news("candidate", LocalDateTime.of(2026, 8, 3, 11, 0))
        `when`(stockRepository.findAllCollectionTargets(defaultWatchlistCodes)).thenReturn(listOf(stock))
        `when`(
            client.collect(
                NewsCollectionClient.Request(
                    targetDate,
                    listOf(NewsCollectionClient.StockRef("BRF001", "브리포주식")),
                ),
            ),
        ).thenReturn(NewsCollectionClient.Result(listOf(existing, candidate)))
        `when`(newsRepository.existsByDedupKey("existing")).thenReturn(true)
        `when`(newsRepository.existsByDedupKey("candidate")).thenReturn(false)
        `when`(stockRepository.findByCode("BRF001")).thenReturn(stock)

        tasklet().execute(mock(StepContribution::class.java), mock(ChunkContext::class.java))

        val captor = ArgumentCaptor.forClass(News::class.java)
        verify(newsRepository).save(captureKotlin(captor))
        assertEquals("candidate", captor.value.dedupKey)
        assertEquals(BigDecimal("0.50"), captor.value.importance)
    }

    @Test
    fun `조회 결과가 없으면 정상 종료하고 저장하지 않는다`() {
        `when`(stockRepository.findAllCollectionTargets(defaultWatchlistCodes)).thenReturn(emptyList())
        `when`(client.collect(NewsCollectionClient.Request(targetDate, emptyList())))
            .thenReturn(NewsCollectionClient.Result(emptyList()))

        tasklet().execute(mock(StepContribution::class.java), mock(ChunkContext::class.java))

        verify(newsRepository, never()).save(any(News::class.java))
    }

    @Test
    fun `관심종목이 없어도 기본 워치리스트 종목을 수집 대상으로 넘긴다`() {
        val watchlistStock = Stock.create("005930", "삼성전자", "IT")
        `when`(stockRepository.findAllCollectionTargets(defaultWatchlistCodes)).thenReturn(listOf(watchlistStock))
        `when`(
            client.collect(
                NewsCollectionClient.Request(
                    targetDate,
                    listOf(NewsCollectionClient.StockRef("005930", "삼성전자")),
                ),
            ),
        ).thenReturn(NewsCollectionClient.Result(emptyList()))

        tasklet().execute(mock(StepContribution::class.java), mock(ChunkContext::class.java))

        verify(client).collect(
            NewsCollectionClient.Request(
                targetDate,
                listOf(NewsCollectionClient.StockRef("005930", "삼성전자")),
            ),
        )
    }

    private fun tasklet() =
        NewsCollectionTasklet(
            targetDate = targetDate,
            client = client,
            stockRepository = stockRepository,
            newsRepository = newsRepository,
            importanceCalculator = NewsImportanceCalculator(),
            defaultWatchlistCodes = defaultWatchlistCodes,
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
