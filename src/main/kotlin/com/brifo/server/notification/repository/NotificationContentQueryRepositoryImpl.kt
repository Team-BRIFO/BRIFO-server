package com.brifo.server.notification.repository

import com.brifo.server.agent.entity.QAgent.Companion.agent
import com.brifo.server.ap.entity.ApTransactionReason
import com.brifo.server.ap.entity.ApTransactionTargetType
import com.brifo.server.ap.entity.QApTransaction.Companion.apTransaction
import com.brifo.server.briefing.entity.QBriefing.Companion.briefing
import com.brifo.server.briefing.entity.QBriefingNewsCard.Companion.briefingNewsCard
import com.brifo.server.decision.entity.QDecision.Companion.decision
import com.brifo.server.decision.entity.QDecisionResult.Companion.decisionResult
import com.brifo.server.news.entity.QNews.Companion.news
import com.brifo.server.news.entity.QNewsCard.Companion.newsCard
import com.brifo.server.stock.entity.QStock.Companion.stock
import com.brifo.server.stock.entity.QUserStock.Companion.userStock
import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

@Repository
class NotificationContentQueryRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : NotificationContentQueryRepository {
    override fun findDecisionResultContent(
        userPublicId: UUID,
        decisionPublicId: UUID,
    ): NotificationContentProjection.DecisionResult? =
        queryFactory
            .select(
                Projections.constructor(
                    NotificationContentProjection.DecisionResult::class.java,
                    decisionResult.dailyStockPrice.stock.name,
                    decisionResult.dailyStockPrice.changeRate,
                    decision.direction,
                    decisionResult.isCorrect,
                    apTransaction.amount,
                    decision.briefing.agent.user.balanceAp,
                ),
            ).from(decisionResult)
            .join(decisionResult.decision, decision)
            .join(apTransaction)
            .on(
                apTransaction.user.eq(decision.briefing.agent.user),
                apTransaction.targetType.eq(ApTransactionTargetType.DECISION),
                apTransaction.targetId.eq(decision.id),
                apTransaction.reason.`in`(DECISION_REASONS),
            ).where(
                decision.briefing.agent.user.publicId.eq(userPublicId),
                decision.publicId.eq(decisionPublicId),
            ).orderBy(apTransaction.id.desc())
            .fetchFirst()

    override fun findBriefingReadyContent(
        userPublicId: UUID,
        briefingPublicId: UUID,
    ): NotificationContentProjection.BriefingReady? =
        queryFactory
            .select(
                Projections.constructor(
                    NotificationContentProjection.BriefingReady::class.java,
                    briefing.agent.agentType,
                    briefingNewsCard.newsCard.news.stock.name,
                    briefing.direction,
                    briefing.confidenceRate,
                ),
            ).from(briefingNewsCard)
            .join(briefingNewsCard.briefing, briefing)
            .where(
                briefing.agent.user.publicId.eq(userPublicId),
                briefing.publicId.eq(briefingPublicId),
                briefing.direction.isNotNull,
                briefing.confidenceRate.isNotNull,
            ).orderBy(briefingNewsCard.id.asc())
            .fetchFirst()

    override fun findNewsCardContents(
        userPublicId: UUID,
        stockPublicId: UUID,
        displayDate: LocalDate,
    ): List<NotificationContentProjection.NewsCard> =
        queryFactory
            .select(Projections.constructor(NotificationContentProjection.NewsCard::class.java, stock.name))
            .from(newsCard)
            .join(newsCard.news, news)
            .join(news.stock, stock)
            .join(userStock)
            .on(userStock.stock.eq(stock))
            .where(
                userStock.user.publicId.eq(userPublicId),
                stock.publicId.eq(stockPublicId),
                newsCard.displayDate.eq(displayDate),
            ).orderBy(newsCard.id.asc())
            .fetch()

    override fun findAgentSalaryContents(
        userPublicId: UUID,
        stockPublicId: UUID,
        displayDate: LocalDate,
    ): List<NotificationContentProjection.AgentSalary> =
        queryFactory
            .select(
                Projections.constructor(
                    NotificationContentProjection.AgentSalary::class.java,
                    agent.agentType,
                    Expressions.numberTemplate(
                        Int::class.javaObjectType,
                        "coalesce(sum({0}), 0)",
                        apTransaction.amount,
                    ),
                    agent.user.balanceAp,
                ),
            ).from(briefing)
            .join(briefing.agent, agent)
            .join(apTransaction)
            .on(
                apTransaction.user.eq(agent.user),
                apTransaction.targetType.eq(ApTransactionTargetType.BRIEFING),
                apTransaction.targetId.eq(briefing.id),
                apTransaction.reason.`in`(ApTransactionReason.SALARY, ApTransactionReason.SALARY_REFUND),
            ).where(
                agent.user.publicId.eq(userPublicId),
                JPAExpressions
                    .selectOne()
                    .from(briefingNewsCard)
                    .where(
                        briefingNewsCard.briefing.eq(briefing),
                        briefingNewsCard.newsCard.news.stock.publicId.eq(stockPublicId),
                        briefingNewsCard.newsCard.displayDate.eq(displayDate),
                    ).exists(),
            ).groupBy(briefing.id, agent.agentType, agent.user.balanceAp)
            .orderBy(briefing.id.asc())
            .fetch()

    companion object {
        private val DECISION_REASONS =
            listOf(
                ApTransactionReason.DECISION_WIN,
                ApTransactionReason.DECISION_LOSE,
                ApTransactionReason.NEUTRAL_HIT,
            )
    }
}
