package com.brifo.server.decision.repository

import com.brifo.server.decision.DecisionMarketPolicy
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
import java.time.LocalDateTime
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
    ): List<GetDecisionsResponse.DecisionItem> {
        val latestPrice = QDailyStockPrice("latestPrice")
        val latestFetchedPrice = QDailyStockPrice("latestFetchedPrice")
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
                    GetDecisionsResponse.DecisionItem::class.java,
                    decision.publicId,
                    decision.direction,
                    decision.confidenceLevel.intValue(),
                    Projections.constructor(
                        GetDecisionsResponse.DecisionListAgent::class.java,
                        decision.briefing.agent.publicId,
                        decision.briefing.agent.agentType,
                    ),
                    Projections.constructor(
                        GetDecisionsResponse.DecisionListStock::class.java,
                        briefingNewsCard.newsCard.news.stock.publicId,
                        briefingNewsCard.newsCard.news.stock.name,
                        briefingNewsCard.newsCard.news.stock.logoUrl,
                        price,
                        roundedChangeRate,
                        dailyStockPrice.tradeDate,
                    ),
                ),
            ).from(decision)
            .join(decision.briefing.briefingNewsCards, briefingNewsCard)
            .join(dailyStockPrice)
            .on(
                dailyStockPrice.stock.eq(briefingNewsCard.newsCard.news.stock),
                dailyStockPrice.tradeDate.eq(
                    JPAExpressions
                        .select(latestPrice.tradeDate.max())
                        .from(latestPrice)
                        .where(latestPrice.stock.eq(briefingNewsCard.newsCard.news.stock)),
                ),
                dailyStockPrice.fetchedAt.eq(
                    JPAExpressions
                        .select(latestFetchedPrice.fetchedAt.max())
                        .from(latestFetchedPrice)
                        .where(
                            latestFetchedPrice.stock.eq(briefingNewsCard.newsCard.news.stock),
                            latestFetchedPrice.tradeDate.eq(dailyStockPrice.tradeDate),
                        ),
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
                        GetDecisionResultResponse.DecisionResultAgent::class.java,
                        decision.briefing.agent.publicId,
                        decision.briefing.agent.agentType,
                    ),
                    Projections.constructor(
                        GetDecisionResultResponse.DecisionResultStock::class.java,
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

    override fun findRecentSettledDecisions(
        userPublicId: UUID,
        limit: Long,
    ): List<RecentSettledDecision> =
        queryFactory
            .select(
                Projections.constructor(
                    RecentSettledDecision::class.java,
                    decisionResult.dailyStockPrice.stock.name,
                    decision.direction,
                    decision.confidenceLevel.intValue(),
                    decisionResult.isCorrect,
                    decisionResult.dailyStockPrice.changeRate,
                ),
            ).from(decisionResult)
            .join(decisionResult.decision, decision)
            .where(decision.briefing.agent.user.publicId.eq(userPublicId))
            .orderBy(decision.createdAt.desc(), decision.id.desc())
            .limit(limit)
            .fetch()

    override fun countByUserIdWithinPeriod(
        userId: Long,
        from: LocalDateTime,
        to: LocalDateTime,
    ): Long =
        queryFactory
            .select(decision.count())
            .from(decision)
            .where(
                decision.briefing.agent.user.id.eq(userId),
                decision.createdAt.goe(from),
                decision.createdAt.lt(to),
            ).fetchOne() ?: 0L

    override fun findUnsettledIds(targetDate: LocalDate): List<Long> =
        queryFactory
            .select(decision.id)
            .from(decision)
            .where(
                JPAExpressions
                    .selectOne()
                    .from(briefingNewsCard)
                    .where(
                        briefingNewsCard.briefing.eq(decision.briefing),
                        briefingNewsCard.newsCard.displayDate.eq(targetDate),
                    ).exists(),
                JPAExpressions
                    .selectOne()
                    .from(decisionResult)
                    .where(decisionResult.decision.eq(decision))
                    .notExists(),
                decision.createdAt.lt(DecisionMarketPolicy.settlementCutoff(targetDate)),
            ).orderBy(decision.createdAt.asc(), decision.id.asc())
            .fetch()

    override fun findUnsettledStockIds(targetDate: LocalDate): List<Long> =
        queryFactory
            .select(briefingNewsCard.newsCard.news.stock.id)
            .distinct()
            .from(decision)
            .join(decision.briefing.briefingNewsCards, briefingNewsCard)
            .where(
                briefingNewsCard.newsCard.displayDate.eq(targetDate),
                JPAExpressions
                    .selectOne()
                    .from(decisionResult)
                    .where(decisionResult.decision.eq(decision))
                    .notExists(),
                decision.createdAt.lt(DecisionMarketPolicy.settlementCutoff(targetDate)),
            ).orderBy(briefingNewsCard.newsCard.news.stock.id.asc())
            .fetch()
}
