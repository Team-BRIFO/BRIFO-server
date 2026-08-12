package com.brifo.server.batch.dev

import com.brifo.server.batch.collection.CollectionRound
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DevBatchCleanupServiceTest {
    private val jdbc = mock(NamedParameterJdbcTemplate::class.java)
    private val queryRepository = mock(DevBatchCleanupQueryRepository::class.java)
    private val service = DevBatchCleanupService(jdbc, queryRepository)

    @Test
    fun `수집 정리는 파생 데이터를 FK 역순으로 삭제한다`() {
        val targetDate = LocalDate.of(2026, 8, 6)
        `when`(
            queryRepository.findNewsIdsPublishedBetween(
                targetDate.atStartOfDay(),
                CollectionRound.CLOSING.cutoffAt(targetDate),
            ),
        ).thenReturn(listOf(1L))
        `when`(queryRepository.findCardIds(listOf(1L))).thenReturn(listOf(2L))
        `when`(queryRepository.findBriefingIds(listOf(2L))).thenReturn(listOf(3L))
        `when`(queryRepository.findBriefingIdsWithoutCards(listOf(3L))).thenReturn(listOf(3L))
        `when`(queryRepository.findDecisionIds(listOf(3L))).thenReturn(listOf(4L))
        `when`(queryRepository.findCardNotificationTargetIds(listOf(2L))).thenReturn(listOf(UUID.randomUUID()))
        `when`(queryRepository.findCardDisplayDates(listOf(2L))).thenReturn(listOf(LocalDate.of(2026, 8, 7)))
        `when`(jdbc.update(any(String::class.java), any(MapSqlParameterSource::class.java))).thenReturn(1)

        val result = service.cleanupForCollection(targetDate, CollectionRound.CLOSING)

        assertEquals(1, result.news)
        assertEquals(1, result.newsCards)
        assertEquals(1, result.briefings)
        assertEquals(1, result.decisions)
        assertEquals(1, result.settlements)

        val sqlCaptor = ArgumentCaptor.forClass(String::class.java)
        verify(jdbc, atLeastOnce()).update(sqlCaptor.capture(), any(MapSqlParameterSource::class.java))
        val sql = sqlCaptor.allValues.map { it.replace(Regex("\\s+"), " ").trim() }
        assertBefore(sql, "DELETE FROM decision_results", "DELETE FROM decisions")
        assertBefore(sql, "DELETE FROM decisions", "DELETE FROM briefings")
        assertBefore(sql, "DELETE FROM briefings", "DELETE FROM news_cards")
        assertBefore(sql, "DELETE FROM news_cards", "DELETE FROM news WHERE")
    }

    @Test
    fun `수집 정리는 공유 브리핑의 다른 카드와 파생 데이터를 유지한다`() {
        val targetDate = LocalDate.of(2026, 8, 6)
        val cardId = 2L
        val sharedBriefingId = 3L
        `when`(
            queryRepository.findNewsIdsPublishedBetween(
                targetDate.atStartOfDay(),
                CollectionRound.CLOSING.cutoffAt(targetDate),
            ),
        ).thenReturn(listOf(1L))
        `when`(queryRepository.findCardIds(listOf(1L))).thenReturn(listOf(cardId))
        `when`(queryRepository.findBriefingIds(listOf(cardId))).thenReturn(listOf(sharedBriefingId))
        `when`(queryRepository.findBriefingIdsWithoutCards(listOf(sharedBriefingId))).thenReturn(emptyList())
        `when`(queryRepository.findCardNotificationTargetIds(listOf(cardId))).thenReturn(listOf(UUID.randomUUID()))
        `when`(queryRepository.findCardDisplayDates(listOf(cardId))).thenReturn(listOf(LocalDate.of(2026, 8, 7)))
        `when`(jdbc.update(any(String::class.java), any(MapSqlParameterSource::class.java))).thenReturn(1)

        val result = service.cleanupForCollection(targetDate, CollectionRound.CLOSING)

        assertEquals(1, result.newsCards)
        assertEquals(0, result.briefings)
        assertEquals(0, result.decisions)
        assertEquals(0, result.settlements)
        verify(queryRepository, never()).findCardIdsByBriefingIds(anyList())
        verify(queryRepository, never()).findDecisionIds(anyList())

        val sqlCaptor = ArgumentCaptor.forClass(String::class.java)
        verify(jdbc, atLeastOnce()).update(sqlCaptor.capture(), any(MapSqlParameterSource::class.java))
        val sql = sqlCaptor.allValues.map { it.replace(Regex("\\s+"), " ").trim() }
        assertTrue(sql.any { "DELETE FROM briefing_news_cards WHERE card_id IN (:ids)" in it })
        assertTrue(sql.none { "DELETE FROM decisions" in it })
        assertTrue(sql.none { "DELETE FROM briefings" in it })
    }

    @Test
    fun `단일 생성 정리는 요청 뉴스의 카드만 삭제한다`() {
        val newsId = 1L
        val cardId = 2L
        val briefingId = 3L
        val decisionId = 4L
        `when`(queryRepository.findCardIds(listOf(newsId))).thenReturn(listOf(cardId))
        `when`(queryRepository.findBriefingIds(listOf(cardId))).thenReturn(listOf(briefingId))
        `when`(queryRepository.findDecisionIds(listOf(briefingId))).thenReturn(listOf(decisionId))
        `when`(queryRepository.findCardNotificationTargetIds(listOf(cardId))).thenReturn(listOf(UUID.randomUUID()))
        `when`(queryRepository.findCardDisplayDates(listOf(cardId))).thenReturn(listOf(LocalDate.of(2026, 8, 7)))
        `when`(jdbc.update(any(String::class.java), any(MapSqlParameterSource::class.java))).thenReturn(1)

        val result = service.cleanupForSingleGeneration(newsId)

        assertEquals(1, result.newsCards)
        assertEquals(1, result.briefings)
        assertEquals(1, result.decisions)
        assertEquals(1, result.settlements)
        verify(queryRepository, never()).findCardIdsByBriefingIds(anyList())

        val sqlCaptor = ArgumentCaptor.forClass(String::class.java)
        verify(jdbc, atLeastOnce()).update(sqlCaptor.capture(), any(MapSqlParameterSource::class.java))
        val sql = sqlCaptor.allValues.map { it.replace(Regex("\\s+"), " ").trim() }
        assertBefore(sql, "DELETE FROM briefing_news_cards", "DELETE FROM briefings")
        assertBefore(sql, "DELETE FROM briefings", "DELETE FROM news_cards")
    }

    private fun assertBefore(
        sql: List<String>,
        first: String,
        second: String,
    ) {
        val firstIndex = sql.indexOfFirst { first in it }
        val secondIndex = sql.indexOfFirst { second in it }
        assertTrue(firstIndex >= 0, "SQL not found: $first")
        assertTrue(secondIndex >= 0, "SQL not found: $second")
        assertTrue(firstIndex < secondIndex, "$first must run before $second")
    }
}
