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

    /**
     * 그 종목에 카드가 있는 가장 최근 노출일(기준일 포함, 그 이전까지).
     *
     * 카드는 전날 뉴스로 하루 한 번 만들어지고 뉴스 1건당 한 장만 만들어지므로, 어제 그 종목에
     * 새 뉴스가 없었으면 오늘 카드는 0장이 된다. 그때 화면을 비워두는 대신 마지막으로 만들어둔
     * 카드를 보여주기 위해 쓴다. 카드마다 발행일(publishedDate)을 함께 내려주므로 이용자가
     * 언제 기사인지 오해하지 않는다.
     */
    fun findLatestDisplayDateOnOrBefore(
        stockPublicId: UUID,
        displayDate: LocalDate,
    ): LocalDate?

    fun findDistinctStockIdsByDisplayDate(displayDate: LocalDate): List<Long>
}
