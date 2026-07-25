package com.brifo.server.user.repository

import com.brifo.server.news.entity.QNews.Companion.news
import com.brifo.server.news.entity.QNewsCard.Companion.newsCard
import com.brifo.server.stock.entity.QDailyStockPrice
import com.brifo.server.stock.entity.QStock.Companion.stock
import com.brifo.server.stock.entity.QUserStock.Companion.userStock
import com.querydsl.core.types.Projections
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDateTime

@Repository
class UserHomeQueryRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
    private val jdbcTemplate: JdbcTemplate,
) : UserHomeQueryRepository {
    override fun findTodayNewsCards(
        userId: Long,
        todayStart: LocalDateTime,
        tomorrowStart: LocalDateTime,
    ): List<UserHomeNewsCard> {
        val latestPrice = QDailyStockPrice("latestPrice")
        val latestPriceSubquery = QDailyStockPrice("latestPriceSubquery")

        return queryFactory
            .select(
                Projections.constructor(
                    UserHomeNewsCard::class.java,
                    newsCard.publicId,
                    newsCard.headline,
                    news.publicId,
                    news.publishedAt,
                    news.source,
                    stock.publicId,
                    stock.name,
                    latestPrice.changeRate,
                ),
            ).from(userStock)
            .join(userStock.stock, stock)
            .join(news).on(news.stock.eq(stock))
            .join(newsCard).on(newsCard.news.eq(news))
            .leftJoin(latestPrice)
            .on(
                latestPrice.stock.eq(stock),
                latestPrice.tradeDate.eq(
                    JPAExpressions
                        .select(latestPriceSubquery.tradeDate.max())
                        .from(latestPriceSubquery)
                        .where(latestPriceSubquery.stock.eq(stock)),
                ),
            ).where(
                userStock.user.id.eq(userId),
                news.publishedAt.goe(todayStart),
                news.publishedAt.lt(tomorrowStart),
            ).orderBy(news.publishedAt.desc(), newsCard.publicId.desc())
            .limit(NEWS_CARD_LIMIT)
            .fetch()
    }

    override fun findLatestCompletedBatchTime(
        todayStart: LocalDateTime,
        tomorrowStart: LocalDateTime,
    ): LocalDateTime? =
        jdbcTemplate
            .query(
                LATEST_COMPLETED_BATCH_QUERY,
                { resultSet, _ -> resultSet.getTimestamp("END_TIME").toLocalDateTime() },
                Timestamp.valueOf(todayStart),
                Timestamp.valueOf(tomorrowStart),
            ).firstOrNull()

    companion object {
        private const val NEWS_CARD_LIMIT = 10L
        private const val LATEST_COMPLETED_BATCH_QUERY =
            """
            SELECT END_TIME
            FROM BATCH_JOB_EXECUTION
            WHERE STATUS = 'COMPLETED'
              AND END_TIME >= ?
              AND END_TIME < ?
            ORDER BY END_TIME DESC
            LIMIT 1
            """
    }
}
