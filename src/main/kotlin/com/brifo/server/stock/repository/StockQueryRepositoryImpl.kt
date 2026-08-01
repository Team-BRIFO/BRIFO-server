package com.brifo.server.stock.repository

import com.brifo.server.stock.dto.response.GetStocksResponse
import com.brifo.server.stock.entity.QDailyStockPrice
import com.brifo.server.stock.entity.QDailyStockPrice.Companion.dailyStockPrice
import com.brifo.server.stock.entity.QStock.Companion.stock
import com.brifo.server.stock.entity.QUserStock.Companion.userStock
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class StockQueryRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : StockQueryRepository {
    override fun findPopularStocks(): List<GetStocksResponse.StockItem> {
        val latestPrice = QDailyStockPrice("latestPrice")

        val latestTradeDate =
            JPAExpressions
                .select(latestPrice.tradeDate.max())
                .from(latestPrice)
                .where(latestPrice.stock.eq(stock))

        val selectedFields =
            Projections.constructor(
                GetStocksResponse.StockItem::class.java,
                Expressions.nullExpression(Int::class.javaObjectType),
                stock.publicId,
                stock.code,
                stock.name,
                dailyStockPrice.price,
                dailyStockPrice.changeRate,
            )

        val query =
            queryFactory
                .select(selectedFields)
                .from(stock)
                .leftJoin(userStock)
                .on(userStock.stock.eq(stock))
                .leftJoin(dailyStockPrice)
                .on(
                    dailyStockPrice.stock.eq(stock),
                    dailyStockPrice.tradeDate.eq(latestTradeDate),
                ).where(stock.isActive.isTrue)
                .groupBy(
                    stock.publicId,
                    stock.code,
                    stock.name,
                    dailyStockPrice.price,
                    dailyStockPrice.changeRate,
                ).orderBy(
                    userStock.id.count().desc(),
                    stock.code.asc(),
                ).limit(5)

        return query.fetch()
    }

    override fun searchStocks(
        keyword: String,
        cursor: UUID?,
        limit: Int,
    ): List<GetStocksResponse.StockItem> {
        val latestPrice = QDailyStockPrice("latestPrice")

        val latestTradeDate =
            JPAExpressions
                .select(latestPrice.tradeDate.max())
                .from(latestPrice)
                .where(latestPrice.stock.eq(stock))

        val normalizedName =
            Expressions.stringTemplate(
                "lower(replace({0}, ' ', ''))",
                stock.name,
            )
        val normalizedKeyword = keyword.replace(" ", "").lowercase()

        val conditions =
            BooleanBuilder()
                .and(stock.isActive.isTrue)
                .and(normalizedName.contains(normalizedKeyword))

        cursor?.let {
            conditions.and(stock.publicId.lt(it))
        }

        val selectedFields =
            Projections.constructor(
                GetStocksResponse.StockItem::class.java,
                Expressions.nullExpression(Int::class.javaObjectType),
                stock.publicId,
                stock.code,
                stock.name,
                dailyStockPrice.price,
                dailyStockPrice.changeRate,
            )

        val query =
            queryFactory
                .select(selectedFields)
                .from(stock)
                .leftJoin(dailyStockPrice)
                .on(
                    dailyStockPrice.stock.eq(stock),
                    dailyStockPrice.tradeDate.eq(latestTradeDate),
                ).where(conditions)
                .orderBy(stock.publicId.desc())
                .limit(limit.toLong())

        return query.fetch()
    }
}
