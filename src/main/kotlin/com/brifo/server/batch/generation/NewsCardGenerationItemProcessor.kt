package com.brifo.server.batch.generation

import com.brifo.server.news.client.NewsCardGenerationClient
import com.brifo.server.news.entity.News
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
                newsContent = buildNewsContent(news),
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

    /**
     * 제목과 요약을 함께 넘긴다.
     *
     * `news.summary`는 네이버 검색 API의 `description`이라 기사 본문이 아니라 검색 스니펫이다.
     * 보통은 리드 문단이 잡히지만 기자명·매체명·번역 안내 같은 기사 하단 정보가 잡히는 기사도 있다.
     * 예전에는 `summary ?: title`이라 요약이 있으면 제목을 버렸고, 그런 기사에서는 AI가 하단 정보만
     * 보고 "김정환 기자가 작성한 기사이다" 같은 카드를 만들어냈다.
     */
    private fun buildNewsContent(news: News): String =
        listOfNotNull(news.title, news.summary?.takeIf { it.isNotBlank() })
            .joinToString(separator = "\n")

    private companion object {
        const val EXCLUDE_TERM_DAYS = 14L
    }
}
