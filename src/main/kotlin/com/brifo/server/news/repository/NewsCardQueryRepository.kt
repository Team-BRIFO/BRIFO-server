package com.brifo.server.news.repository

import com.brifo.server.news.entity.NewsCard
import java.time.LocalDate
import java.util.UUID

interface NewsCardQueryRepository {
    /** 사원이 분석할 카드를 고른다. 분석에 넣는 장수가 정해져 있어 상한이 있다. */
    fun findAnalysisCards(
        stockPublicId: UUID,
        displayDate: LocalDate,
    ): List<NewsCard>

    /** 카드뉴스 상세 화면에 보여줄, 그날 그 종목의 카드 전부. */
    fun findDisplayCards(
        stockPublicId: UUID,
        displayDate: LocalDate,
    ): List<NewsCard>

    fun findDistinctStockIdsByDisplayDate(displayDate: LocalDate): List<Long>
}
