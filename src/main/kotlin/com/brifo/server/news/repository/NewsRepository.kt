package com.brifo.server.news.repository

import com.brifo.server.news.entity.News
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.UUID

interface NewsRepository : JpaRepository<News, Long> {
    fun findByPublicId(publicId: UUID): News?

    fun existsByDedupKey(dedupKey: String): Boolean

    // QueryDSL JPA는 종목별 순위를 위한 window function을 직접 지원하지 않아 native query로 유지한다.
    @Query(
        value =
            """
            SELECT ranked.id
            FROM (
                SELECT n.id,
                       n.stock_id,
                       ROW_NUMBER() OVER (
                           PARTITION BY n.stock_id
                           ORDER BY n.importance DESC NULLS LAST, n.published_at DESC, n.id ASC
                       ) AS rank
                FROM news n
                WHERE n.published_at >= :from
                  AND n.published_at < :to
            ) ranked
            WHERE ranked.rank <= 2
              AND NOT EXISTS (
                  SELECT 1 FROM news_cards nc WHERE nc.news_id = ranked.id
              )
            ORDER BY ranked.stock_id ASC, ranked.rank ASC
            """,
        nativeQuery = true,
    )
    fun findGenerationCandidateIds(
        @Param("from") from: LocalDateTime,
        @Param("to") to: LocalDateTime,
    ): List<Long>
}
