package com.brifo.server.batch.generation

import com.brifo.server.news.entity.NewsCard
import com.brifo.server.news.entity.ImportanceBadge
import com.brifo.server.news.repository.NewsCardRepository
import com.brifo.server.news.repository.NewsRepository
import com.brifo.server.term.entity.GlossaryTerm
import com.brifo.server.term.entity.NewsCardTerm
import com.brifo.server.term.repository.GlossaryTermRepository
import com.brifo.server.term.repository.NewsCardTermRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

@Service
class NewsCardPersistenceService(
    private val newsRepository: NewsRepository,
    private val newsCardRepository: NewsCardRepository,
    private val glossaryTermRepository: GlossaryTermRepository,
    private val newsCardTermRepository: NewsCardTermRepository,
) {
    @Transactional
    fun save(
        item: GeneratedNewsCardItem,
        displayDate: LocalDate,
    ) {
        if (newsCardRepository.existsByNewsId(item.newsId)) return

        val news = newsRepository.findById(item.newsId).orElseThrow {
            IllegalStateException("뉴스를 찾을 수 없습니다: ${item.newsId}")
        }
        val generated = item.card
        require(generated.headline.isNotBlank()) { "생성된 headline은 비어 있을 수 없습니다." }
        require(generated.points.isNotEmpty()) { "생성된 points는 비어 있을 수 없습니다." }

        val card = newsCardRepository.save(
            NewsCard.create(
                news = news,
                headline = generated.headline,
                points = generated.points,
                keywords = generated.keywords,
                importanceBadge = importanceBadge(news.importance),
                displayDate = displayDate,
            ),
        )

        generated.terms
            .distinctBy { it.term }
            .forEachIndexed { index, generatedTerm ->
                val term = glossaryTermRepository.findByTerm(generatedTerm.term)
                    ?: glossaryTermRepository.save(
                        GlossaryTerm.create(
                            term = generatedTerm.term,
                            definition = generatedTerm.definition,
                            category = AI_TERM_CATEGORY,
                        ),
                    )
                newsCardTermRepository.save(
                    NewsCardTerm.create(
                        newsCard = card,
                        term = term,
                        surface = generatedTerm.surface,
                        displayOrder = index,
                    ),
                )
            }
        news.markProcessed()
    }

    private fun importanceBadge(importance: BigDecimal?): ImportanceBadge =
        when {
            importance == null || importance < MID_IMPORTANCE -> ImportanceBadge.LOW
            importance < HOT_IMPORTANCE -> ImportanceBadge.MID
            else -> ImportanceBadge.HOT
        }

    private companion object {
        const val AI_TERM_CATEGORY = "AI"
        val MID_IMPORTANCE = BigDecimal("0.4")
        val HOT_IMPORTANCE = BigDecimal("0.7")
    }
}
