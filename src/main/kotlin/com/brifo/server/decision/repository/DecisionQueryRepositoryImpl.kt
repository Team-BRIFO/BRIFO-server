package com.brifo.server.decision.repository

import com.brifo.server.ap.entity.ApTransactionTargetType
import com.brifo.server.ap.entity.QApTransaction.Companion.apTransaction
import com.brifo.server.briefing.entity.QBriefingNewsCard
import com.brifo.server.briefing.entity.QBriefingNewsCard.Companion.briefingNewsCard
import com.brifo.server.decision.dto.response.GetDecisionResultResponse
import com.brifo.server.decision.dto.response.GetDecisionsResponse
import com.brifo.server.decision.entity.QDecision.Companion.decision
import com.brifo.server.decision.entity.QDecisionResult.Companion.decisionResult
import com.brifo.server.stock.entity.QDailyStockPrice
import com.brifo.server.stock.entity.QDailyStockPrice.Companion.dailyStockPrice
import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Repository
class DecisionQueryRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : DecisionQueryRepository {
    override fun existsDailyDecision(
        userPublicId: UUID,
        stockPublicId: UUID,
        displayDate: LocalDate,
    ): Boolean =
        queryFactory
            .selectOne()
            .from(decision)
            .join(decision.briefing.briefingNewsCards, briefingNewsCard)
            .where(
                decision.briefing.agent.user.publicId.eq(userPublicId),
                briefingNewsCard.newsCard.news.stock.publicId.eq(stockPublicId),
                briefingNewsCard.newsCard.displayDate.eq(displayDate),
            ).fetchFirst() != null

    override fun findTodayUnsettledDecisions(
        userPublicId: UUID,
        displayDate: LocalDate,
    ): List<GetDecisionsResponse.Item> {
        val latestPrice = QDailyStockPrice("latestPrice")
        val firstBriefingNewsCard = QBriefingNewsCard("firstBriefingNewsCard")
        val roundedChangeRate = Expressions.numberTemplate(
            BigDecimal::class.java,
            "round({0}, 1)",
            dailyStockPrice.changeRate,
        )
        val price = Expressions.numberTemplate(
            Long::class.javaObjectType,
            "cast({0} as long)",
            dailyStockPrice.price,
        )
        return queryFactory
            .select(
                Projections.constructor(
                    GetDecisionsResponse.Item::class.java,
                    decision.publicId,
                    decision.direction,
                    decision.confidenceLevel.intValue(),
                    Projections.constructor(
                        GetDecisionsResponse.Agent::class.java,
                        decision.briefing.agent.publicId,
                        decision.briefing.agent.agentType,
                    ),
                    Projections.constructor(
                        GetDecisionsResponse.Stock::class.java,
                        briefingNewsCard.newsCard.news.stock.publicId,
                        briefingNewsCard.newsCard.news.stock.name,
                        price,
                        roundedChangeRate,
                        dailyStockPrice.tradeDate,
                    ),
                ),
            ).from(decision)
            .join(decision.briefing.briefingNewsCards, briefingNewsCard)
            .leftJoin(dailyStockPrice)
            .on(
                dailyStockPrice.stock.eq(briefingNewsCard.newsCard.news.stock),
                dailyStockPrice.tradeDate.eq(
                    JPAExpressions
                        .select(latestPrice.tradeDate.max())
                        .from(latestPrice)
                        .where(latestPrice.stock.eq(briefingNewsCard.newsCard.news.stock)),
                ),
            ).where(
                decision.briefing.agent.user.publicId.eq(userPublicId),
                briefingNewsCard.newsCard.displayDate.eq(displayDate),
                briefingNewsCard.id.eq(
                    JPAExpressions
                        .select(firstBriefingNewsCard.id.min())
                        .from(firstBriefingNewsCard)
                        .where(firstBriefingNewsCard.briefing.eq(decision.briefing)),
                ),
                JPAExpressions
                    .selectOne()
                    .from(decisionResult)
                    .where(decisionResult.decision.eq(decision))
                    .notExists(),
            ).orderBy(decision.createdAt.desc(), decision.id.desc())
            .fetch()
    }

    override fun findDecisionResults(
        userPublicId: UUID,
        decisionPublicId: UUID,
    ): List<GetDecisionResultResponse> {
        val roundedChangeRate = Expressions.numberTemplate(
            BigDecimal::class.java,
            "round({0}, 1)",
            decisionResult.dailyStockPrice.changeRate,
        )
        val price = Expressions.numberTemplate(
            Long::class.javaObjectType,
            "cast({0} as long)",
            decisionResult.dailyStockPrice.price,
        )
        return queryFactory
            .select(
                Projections.constructor(
                    GetDecisionResultResponse::class.java,
                    decisionResult.isCorrect,
                    apTransaction.amount,
                    decision.direction,
                    decision.confidenceLevel.intValue(),
                    Projections.constructor(
                        GetDecisionResultResponse.Agent::class.java,
                        decision.briefing.agent.publicId,
                        decision.briefing.agent.agentType,
                    ),
                    Projections.constructor(
                        GetDecisionResultResponse.Stock::class.java,
                        decisionResult.dailyStockPrice.stock.name,
                        price,
                        roundedChangeRate,
                        decisionResult.dailyStockPrice.tradeDate,
                    ),
                ),
            ).from(decisionResult)
            .join(decisionResult.decision, decision)
            .join(apTransaction)
            .on(
                apTransaction.targetType.eq(ApTransactionTargetType.DECISION),
                apTransaction.targetId.eq(decision.id),
            ).where(
                decision.publicId.eq(decisionPublicId),
                decision.briefing.agent.user.publicId.eq(userPublicId),
            ).fetch()
    }

    override fun findRecentSettledDecisionIds(
        userPublicId: UUID,
        limit: Long,
    ): List<UUID> =
        queryFactory
            .select(decision.publicId)
            .from(decisionResult)
            .join(decisionResult.decision, decision)
            .where(decision.briefing.agent.user.publicId.eq(userPublicId))
            .orderBy(decision.createdAt.desc(), decision.id.desc())
            .limit(limit)
            .fetch()
}
