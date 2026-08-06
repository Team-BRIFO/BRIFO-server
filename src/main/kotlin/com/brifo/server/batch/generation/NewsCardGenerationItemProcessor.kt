package com.brifo.server.batch.generation

import com.brifo.server.news.client.NewsCardGenerationClient
import com.brifo.server.news.repository.NewsRepository
import com.brifo.server.term.repository.NewsCardTermRepository
import org.springframework.batch.infrastructure.item.ItemProcessor

data class GeneratedNewsCardItem(
    val newsId: Long,
    val card: NewsCardGenerationClient.CardNews,
)

class NewsCardGenerationItemProcessor(
    private val newsRepository: NewsRepository,
    private val newsCardTermRepository: NewsCardTermRepository,
    private val client: NewsCardGenerationClient,
) : ItemProcessor<Long, GeneratedNewsCardItem> {
    override fun process(newsId: Long): GeneratedNewsCardItem {
        val news = newsRepository.findById(newsId).orElseThrow { IllegalStateException("뉴스를 찾을 수 없습니다: $newsId") }
        val newsPublicId = requireNotNull(news.publicId) { "저장된 뉴스에는 publicId가 있어야 합니다." }
        val publishedDate = news.publishedAt.toLocalDate()
        val response = client.createNewsCard(
            NewsCardGenerationClient.Request(
                newsId = newsPublicId,
                stockName = news.stock.name,
                newsContent = news.summary ?: news.title,
                excludeTerms = newsCardTermRepository.findTermsUsedBetween(
                    fromInclusive = publishedDate.minusDays(EXCLUDE_TERM_DAYS - 1),
                    toExclusive = publishedDate.plusDays(1),
                ),
            ),
        )
        check(response.newsId == newsPublicId) { "카드뉴스 응답의 newsId가 요청과 다릅니다." }
        val card = response.cardNews.singleOrNull() ?: error("카드뉴스 응답에는 카드가 정확히 한 개 있어야 합니다.")
        return GeneratedNewsCardItem(newsId, card)
    }

    private companion object {
        const val EXCLUDE_TERM_DAYS = 14L
    }
}
