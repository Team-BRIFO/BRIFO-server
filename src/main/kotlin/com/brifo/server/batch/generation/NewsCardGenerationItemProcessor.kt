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
        return GeneratedNewsCardItem(newsId, card.copy(points = dedupeSimilarPoints(card.points)))
    }

    /**
     * AI가 같은 내용을 숫자만 바꿔 반복 생성하는 경우가 있다(예: "1분기"/"2분기"만 다르고
     * 나머지 문장이 동일). 문장을 억지로 3줄 다 채우기보다, 겹치는 줄은 먼저 나온 것만 남긴다.
     */
    private fun dedupeSimilarPoints(points: List<String>): List<String> {
        val kept = mutableListOf<String>()
        for (point in points) {
            if (kept.none { isSimilar(it, point) }) kept.add(point)
        }
        return kept
    }

    private fun isSimilar(
        a: String,
        b: String,
    ): Boolean {
        val tokensA = a.trim().split(WHITESPACE).toSet()
        val tokensB = b.trim().split(WHITESPACE).toSet()
        if (tokensA.isEmpty() || tokensB.isEmpty()) return a.trim() == b.trim()

        val union = tokensA.union(tokensB).size
        if (union == 0) return true
        val intersection = tokensA.intersect(tokensB).size
        return intersection.toDouble() / union >= SIMILARITY_THRESHOLD
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

        /** 공백 기준 토큰 겹침 비율이 이 값 이상이면 같은 내용의 반복으로 본다. */
        const val SIMILARITY_THRESHOLD = 0.7
        val WHITESPACE = Regex("\\s+")
    }
}
