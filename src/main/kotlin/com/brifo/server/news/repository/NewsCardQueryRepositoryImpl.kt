package com.brifo.server.news.repository

import com.brifo.server.news.entity.NewsCard
import com.brifo.server.news.entity.QNews.Companion.news
import com.brifo.server.news.entity.QNewsCard.Companion.newsCard
import com.brifo.server.stock.entity.QStock.Companion.stock
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

@Repository
class NewsCardQueryRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : NewsCardQueryRepository {
    override fun findAnalysisCards(
        stockPublicId: UUID,
        displayDate: LocalDate,
    ): List<NewsCard> =
        queryFactory
            .selectFrom(newsCard)
            .join(newsCard.news, news)
            .fetchJoin()
            .join(news.stock, stock)
            .fetchJoin()
            .where(
                stock.publicId.eq(stockPublicId),
                newsCard.displayDate.eq(displayDate),
            ).orderBy(newsCard.id.desc())
            .limit(NEWS_CARD_LIMIT)
            .fetch()

    override fun findDisplayCards(
        stockPublicId: UUID,
        displayDate: LocalDate,
    ): List<NewsCard> =
        queryFactory
            .selectFrom(newsCard)
            .join(newsCard.news, news)
            .fetchJoin()
            .join(news.stock, stock)
            .fetchJoin()
            .where(
                stock.publicId.eq(stockPublicId),
                newsCard.displayDate.eq(displayDate),
            ).orderBy(newsCard.id.desc())
            .fetch()

    override fun findDistinctStockIdsByDisplayDate(displayDate: LocalDate): List<Long> =
        queryFactory
            .select(newsCard.news.stock.id)
            .distinct()
            .from(newsCard)
            .where(newsCard.displayDate.eq(displayDate))
            .orderBy(newsCard.news.stock.id.asc())
            .fetch()

    private companion object {
        /** 사원 한 명이 분석에 넣는 카드 장수. 상세 화면 목록과는 무관하다. */
        const val NEWS_CARD_LIMIT = 2L
    }
}
