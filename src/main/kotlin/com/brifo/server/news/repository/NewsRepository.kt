package com.brifo.server.news.repository

import com.brifo.server.news.entity.News
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface NewsRepository : JpaRepository<News, Long> {
    fun findByPublicId(publicId: UUID): News?

    fun existsByDedupKey(dedupKey: String): Boolean

    /**
     * 카드뉴스 생성 대상 뉴스 id. 종목별 최신 뉴스에서 `limitPerStock`건까지 뽑는다.
     *
     * 수집 대상(`StockRepository.findAllCollectionTargets`)과 같은 기준으로 종목을 고른다.
     * 관심종목만으로 좁히면 `user_stocks`가 비었을 때 후보가 영원히 0건이 된다.
     *
     * 생성 배치는 하루 한 번 돌기 때문에 이 값이 곧 종목당 하루 카드 장수의 상한이다.
     * 예전에는 2로 하드코딩돼 있어서, 수집 건수를 늘려도 카드는 2장에서 늘지 않았다.
     *
     * QueryDSL JPA는 종목별 순위를 위한 window function을 직접 지원하지 않아 native query로 유지한다.
     */
    @Query(
        value =
            """
            SELECT ranked.id
            FROM (
                SELECT n.id,
                       n.stock_id,
                       ROW_NUMBER() OVER (
                           PARTITION BY n.stock_id
                           ORDER BY n.published_at DESC, n.id DESC
                       ) AS rank
                FROM news n
                JOIN stocks s ON s.id = n.stock_id
                WHERE s.is_active = TRUE
                  AND (
                      EXISTS (SELECT 1 FROM user_stocks us WHERE us.stock_id = n.stock_id)
                      OR s.code IN (:defaultWatchlistCodes)
                  )
                  AND NOT EXISTS (
                      SELECT 1 FROM news_cards nc WHERE nc.news_id = n.id
                  )
            ) ranked
            WHERE ranked.rank <= :limitPerStock
            ORDER BY ranked.stock_id ASC, ranked.rank ASC
            """,
        nativeQuery = true,
    )
    fun findGenerationCandidateIds(
        @Param("defaultWatchlistCodes") defaultWatchlistCodes: Collection<String>,
        @Param("limitPerStock") limitPerStock: Int,
    ): List<Long>
}
