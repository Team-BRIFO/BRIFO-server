package com.brifo.server.decision.repository

import com.brifo.server.decision.entity.QDecision.Companion.decision
import com.brifo.server.decision.entity.QDecisionResult.Companion.decisionResult
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
class DecisionQueryRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : DecisionQueryRepository {
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
}
