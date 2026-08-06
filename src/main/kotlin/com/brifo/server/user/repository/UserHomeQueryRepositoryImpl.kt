package com.brifo.server.user.repository

import com.brifo.server.news.entity.QNews.Companion.news
import com.brifo.server.news.entity.QNewsCard.Companion.newsCard
import com.brifo.server.stock.entity.QDailyStockPrice
import com.brifo.server.stock.entity.QStock.Companion.stock
import com.brifo.server.stock.entity.QUserStock.Companion.userStock
import com.querydsl.core.types.Projections
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class UserHomeQueryRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : UserHomeQueryRepository {
    override fun findTodayNewsCards(
        userId: Long,
        displayDate: LocalDate,
    ): List<UserHomeNewsCard> {
        val latestPrice = QDailyStockPrice("latestPrice")
        val latestPriceSubquery = QDailyStockPrice("latestPriceSubquery")
        val latestFetchedPrice = QDailyStockPrice("latestFetchedPrice")

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
                    stock.logoUrl,
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
                        .where(
                            latestPriceSubquery.stock.eq(stock),
                            latestPriceSubquery.tradeDate.loe(displayDate),
                        ),
                ),
                latestPrice.fetchedAt.eq(
                    JPAExpressions
                        .select(latestFetchedPrice.fetchedAt.max())
                        .from(latestFetchedPrice)
                        .where(
                            latestFetchedPrice.stock.eq(stock),
                            latestFetchedPrice.tradeDate.eq(latestPrice.tradeDate),
                        ),
                ),
            ).where(
                userStock.user.id.eq(userId),
                newsCard.displayDate.eq(displayDate),
            ).orderBy(news.publishedAt.desc(), newsCard.publicId.desc())
            .limit(NEWS_CARD_LIMIT)
            .fetch()
    }

    companion object {
        private const val NEWS_CARD_LIMIT = 10L
    }
}
