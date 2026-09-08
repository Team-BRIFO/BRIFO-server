package com.brifo.server.news.repository

import com.brifo.server.news.entity.NewsCard
import java.time.LocalDate
import java.util.UUID

interface NewsCardQueryRepository {
    /**
     * 그날 그 종목의 카드뉴스 전부. 상세 화면 목록과 사원 분석이 같은 집합을 쓴다.
     *
     * 하루에 만드는 장수는 `app.batch.news-cards-per-stock`(기본 3)이 정한다.
     * 여기서 따로 상한을 두면 생성 설정과 조용히 어긋나므로 두지 않는다.
     */
    fun findDailyCards(
        stockPublicId: UUID,
        displayDate: LocalDate,
    ): List<NewsCard>

    fun findDistinctStockIdsByDisplayDate(displayDate: LocalDate): List<Long>
}
