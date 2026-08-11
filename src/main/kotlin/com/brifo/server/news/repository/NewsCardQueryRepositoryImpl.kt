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
            ).orderBy(newsCard.id.asc())
            .fetch()

    override fun findDistinctStockIdsByDisplayDate(displayDate: LocalDate): List<Long> =
        queryFactory
            .select(newsCard.news.stock.id)
            .distinct()
            .from(newsCard)
            .where(newsCard.displayDate.eq(displayDate))
            .orderBy(newsCard.news.stock.id.asc())
            .fetch()
}
