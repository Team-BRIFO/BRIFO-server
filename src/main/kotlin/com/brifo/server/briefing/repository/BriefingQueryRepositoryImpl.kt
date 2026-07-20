package com.brifo.server.briefing.repository

import com.brifo.server.agent.entity.QAgent.Companion.agent
import com.brifo.server.briefing.dto.response.BriefingStockResponse
import com.brifo.server.briefing.dto.response.GetOfficeBriefingsResponse
import com.brifo.server.briefing.dto.response.GetStockBriefingsResponse
import com.brifo.server.briefing.entity.Briefing
import com.brifo.server.briefing.entity.QBriefing.Companion.briefing
import com.brifo.server.briefing.entity.QBriefingNewsCard.Companion.briefingNewsCard
import com.brifo.server.stock.entity.QDailyStockPrice
import com.brifo.server.stock.entity.QDailyStockPrice.Companion.dailyStockPrice
import com.brifo.server.stock.entity.QStock.Companion.stock
import com.querydsl.core.types.Projections
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Repository
class BriefingQueryRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : BriefingQueryRepository {
    override fun findDailyBriefings(
        userPublicId: UUID,
        stockPublicId: UUID,
        displayDate: LocalDate,
    ): List<Briefing> =
        queryFactory
            .select(briefing)
            .from(briefingNewsCard)
            .join(briefingNewsCard.briefing, briefing)
            .join(briefing.agent, agent)
            .fetchJoin()
            .where(
                briefing.agent.user.publicId.eq(userPublicId),
                briefingNewsCard.newsCard.news.stock.publicId.eq(stockPublicId),
                briefingNewsCard.newsCard.displayDate.eq(displayDate),
            ).distinct()
            .orderBy(briefing.id.asc())
            .fetch()

    override fun findStockBriefingItems(
        userPublicId: UUID,
        stockPublicId: UUID,
        displayDate: LocalDate,
    ): List<GetStockBriefingsResponse.Item> =
        queryFactory
            .select(
                Projections.constructor(
                    GetStockBriefingsResponse.Item::class.java,
                    briefing.publicId,
                    briefing.status,
                    briefing.oneLiner,
                    briefing.direction,
                    briefing.agent.publicId,
                    briefing.agent.nickname,
                    briefing.agent.agentType,
                ),
            ).from(briefing)
            .where(
                briefing.agent.user.publicId.eq(userPublicId),
                JPAExpressions
                    .selectOne()
                    .from(briefingNewsCard)
                    .where(
                        briefingNewsCard.briefing.eq(briefing),
                        briefingNewsCard.newsCard.news.stock.publicId.eq(stockPublicId),
                        briefingNewsCard.newsCard.displayDate.eq(displayDate),
                    ).exists(),
            )
            .orderBy(briefing.id.asc())
            .fetch()

    override fun findOfficeBriefings(
        userPublicId: UUID,
        displayDate: LocalDate,
    ): List<GetOfficeBriefingsResponse.Item> {
        val rows = queryFactory
            .select(
                Projections.constructor(
                    OfficeBriefingRow::class.java,
                    briefingNewsCard.newsCard.news.stock.publicId,
                    briefingNewsCard.newsCard.news.stock.name,
                    briefing.publicId,
                    briefing.agent.publicId,
                    briefing.agent.nickname,
                    briefing.agent.agentType,
                    briefing.status,
                    briefing.createdAt,
                    briefing.id,
                ),
            ).from(briefingNewsCard)
            .join(briefingNewsCard.briefing, briefing)
            .where(
                briefing.agent.user.publicId.eq(userPublicId),
                briefingNewsCard.newsCard.displayDate.eq(displayDate),
            ).distinct()
            .orderBy(briefing.createdAt.desc(), briefing.id.desc())
            .fetch()

        return rows
            .groupBy(OfficeBriefingRow::stockPublicId)
            .values
            .map { stockRows ->
                GetOfficeBriefingsResponse.Item(
                    stockName = stockRows.first().stockName,
                    agents = stockRows.map { row ->
                        GetOfficeBriefingsResponse.Agent(
                            briefingId = row.briefingPublicId,
                            agentId = row.agentPublicId,
                            nickname = row.nickname,
                            agentType = row.agentType,
                            status = row.status,
                        )
                    },
                )
            }
    }

    override fun findStockSummary(stockPublicId: UUID): BriefingStockResponse? {
        val latestPrice = QDailyStockPrice("latestPrice")

        return queryFactory
            .select(
                Projections.constructor(
                    BriefingStockResponse::class.java,
                    stock.publicId,
                    stock.name,
                    dailyStockPrice.price,
                    dailyStockPrice.changeRate,
                    dailyStockPrice.tradeDate,
                ),
            ).from(stock)
            .join(dailyStockPrice)
            .on(dailyStockPrice.stock.eq(stock))
            .where(
                stock.publicId.eq(stockPublicId),
                dailyStockPrice.tradeDate.eq(
                    JPAExpressions
                        .select(latestPrice.tradeDate.max())
                        .from(latestPrice)
                        .where(latestPrice.stock.eq(stock)),
                ),
            ).fetchOne()
    }

    override fun findOwnerPublicId(briefingPublicId: UUID): UUID? =
        queryFactory
            .select(briefing.agent.user.publicId)
            .from(briefing)
            .where(briefing.publicId.eq(briefingPublicId))
            .fetchOne()

    override fun findOwnedBriefing(
        userPublicId: UUID,
        briefingPublicId: UUID,
    ): Briefing? =
        queryFactory
            .select(briefing)
            .from(briefing)
            .join(briefing.agent, agent)
            .fetchJoin()
            .join(briefing.briefingNewsCards, briefingNewsCard)
            .fetchJoin()
            .where(
                briefing.agent.user.publicId.eq(userPublicId),
                briefing.publicId.eq(briefingPublicId),
            ).distinct()
            .fetchOne()

}

data class OfficeBriefingRow(
    val stockPublicId: UUID,
    val stockName: String,
    val briefingPublicId: UUID,
    val agentPublicId: UUID,
    val nickname: String,
    val agentType: com.brifo.server.agent.entity.AgentType,
    val status: com.brifo.server.briefing.entity.BriefingStatus,
    val createdAt: LocalDateTime,
    val briefingId: Long,
)
