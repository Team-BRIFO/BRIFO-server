package com.brifo.server.batch.generation

import com.brifo.server.batch.captureKotlin
import com.brifo.server.news.client.NewsCardGenerationClient
import com.brifo.server.news.entity.ImportanceBadge
import com.brifo.server.news.entity.News
import com.brifo.server.news.entity.NewsCard
import com.brifo.server.news.repository.NewsCardRepository
import com.brifo.server.news.repository.NewsRepository
import com.brifo.server.term.entity.GlossaryTerm
import com.brifo.server.term.entity.NewsCardTerm
import com.brifo.server.term.repository.GlossaryTermRepository
import com.brifo.server.term.repository.NewsCardTermRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional
import kotlin.test.assertEquals

class NewsCardPersistenceServiceTest {
    private val newsRepository = mock(NewsRepository::class.java)
    private val cardRepository = mock(NewsCardRepository::class.java)
    private val glossaryRepository = mock(GlossaryTermRepository::class.java)
    private val cardTermRepository = mock(NewsCardTermRepository::class.java)
    private val service = NewsCardPersistenceService(newsRepository, cardRepository, glossaryRepository, cardTermRepository)

    @BeforeEach
    fun setUp() {
        `when`(cardRepository.save(any(NewsCard::class.java))).thenAnswer { it.getArgument(0) }
        `when`(glossaryRepository.save(any(GlossaryTerm::class.java))).thenAnswer { it.getArgument(0) }
    }

    @Test
    fun `이미 카드가 있는 뉴스는 외부 응답을 다시 저장하지 않는다`() {
        val news = mock(News::class.java)
        `when`(cardRepository.existsByNewsId(1L)).thenReturn(true)
        `when`(newsRepository.findById(1L)).thenReturn(Optional.of(news))

        service.save(item(1L), LocalDate.of(2026, 8, 4))

        verify(cardRepository, never()).save(any(NewsCard::class.java))
        verify(news).markProcessed()
    }

    @Test
    fun `카드와 고유 용어 관계를 저장하고 뉴스를 처리 완료로 변경한다`() {
        val news = mock(News::class.java)
        val existingTerm = mock(GlossaryTerm::class.java)
        `when`(cardRepository.existsByNewsId(1L)).thenReturn(false)
        `when`(newsRepository.findById(1L)).thenReturn(Optional.of(news))
        `when`(news.importance).thenReturn(BigDecimal("0.80"))
        `when`(news.sourceImageUrl).thenReturn("https://cdn.example.com/news/1.webp")
        `when`(glossaryRepository.findByTerm("계약")).thenReturn(existingTerm)

        service.save(item(1L), LocalDate.of(2026, 8, 4))

        val cardCaptor = ArgumentCaptor.forClass(NewsCard::class.java)
        verify(cardRepository).save(captureKotlin(cardCaptor))
        assertEquals(LocalDate.of(2026, 8, 4), cardCaptor.value.displayDate)
        assertEquals(ImportanceBadge.HOT, cardCaptor.value.importanceBadge)
        assertEquals("https://cdn.example.com/news/1.webp", cardCaptor.value.imageUrl)
        verify(cardTermRepository, times(1)).save(any(NewsCardTerm::class.java))
        verify(news).markProcessed()
    }

    private fun item(newsId: Long) =
        GeneratedNewsCardItem(
            newsId = newsId,
            card =
                NewsCardGenerationClient.CardNews(
                    headline = "계약 소식",
                    points = listOf("계약이 체결됐어요"),
                    keywords = listOf("계약"),
                    terms =
                        listOf(
                            NewsCardGenerationClient.GlossaryTerm("계약", "약속"),
                            NewsCardGenerationClient.GlossaryTerm("계약", "중복"),
                        ),
                ),
        )
}
