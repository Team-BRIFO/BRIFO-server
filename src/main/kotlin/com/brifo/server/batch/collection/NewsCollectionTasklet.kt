package com.brifo.server.batch.collection

import com.brifo.server.news.client.DisclosureClient
import com.brifo.server.news.client.NewsCollectionClient
import com.brifo.server.news.entity.News
import com.brifo.server.news.repository.NewsRepository
import com.brifo.server.stock.repository.StockRepository
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.StepContribution
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import java.time.LocalDate

class NewsCollectionTasklet(
    private val targetDate: LocalDate,
    private val collectionRound: CollectionRound,
    private val client: NewsCollectionClient,
    private val disclosureClient: DisclosureClient,
    private val stockRepository: StockRepository,
    private val newsRepository: NewsRepository,
    private val importanceCalculator: NewsImportanceCalculator,
) : Tasklet {
    override fun execute(
        contribution: StepContribution,
        chunkContext: ChunkContext,
    ): RepeatStatus {
        val cutoff = collectionRound.cutoffAt(targetDate)
        val disclosureByStockCode = mutableMapOf<String, Boolean>()
        val stockCodes = stockRepository.findAllByIsActiveTrueOrderByCode().map { it.code }
        client.collect(NewsCollectionClient.Request(targetDate, cutoff, stockCodes)).news
            .asSequence()
            .filter { it.publishedAt.toLocalDate() == targetDate }
            .filter { !it.publishedAt.isAfter(cutoff) }
            .filterNot { newsRepository.existsByDedupKey(it.dedupKey) }
            .forEach { collected ->
                val stock = checkNotNull(stockRepository.findByCode(collected.stockCode)) {
                    "알 수 없는 종목 코드입니다: ${collected.stockCode}"
                }
                val importance = importanceCalculator.calculate(
                    round = CollectionRound.fromPublishedAt(collected.publishedAt),
                    hasDisclosure =
                        disclosureByStockCode.getOrPut(stock.code) {
                            disclosureClient.exists(
                                DisclosureClient.Request(
                                    stockId = stock.id,
                                    stockCode = stock.code,
                                    date = targetDate,
                                ),
                            )
                        },
                )
                newsRepository.save(
                    News.create(
                        stock = stock,
                        source = collected.source,
                        sourceUrl = collected.sourceUrl,
                        sourceImageUrl = collected.sourceImageUrl,
                        title = collected.title,
                        summary = collected.summary,
                        importance = importance,
                        dedupKey = collected.dedupKey,
                        publishedAt = collected.publishedAt,
                    ),
                )
            }
        return RepeatStatus.FINISHED
    }
}
