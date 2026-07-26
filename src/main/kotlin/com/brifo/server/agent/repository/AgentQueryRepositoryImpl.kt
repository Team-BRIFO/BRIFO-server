package com.brifo.server.agent.repository

import com.brifo.server.agent.entity.QAgent.Companion.agent
import com.brifo.server.ap.entity.ApTransactionReason
import com.brifo.server.ap.entity.ApTransactionTargetType
import com.brifo.server.ap.entity.QApTransaction.Companion.apTransaction
import com.brifo.server.briefing.entity.BriefingStatus
import com.brifo.server.briefing.entity.QBriefing.Companion.briefing
import com.brifo.server.decision.entity.QDecision.Companion.decision
import com.brifo.server.decision.entity.QDecisionResult.Companion.decisionResult
import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

@Repository
class AgentQueryRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : AgentQueryRepository {
    override fun findAgentSummaries(userPublicId: UUID): List<AgentQueryRepository.AgentSummary> {
        val totalCount = decisionResult.id.count()
        val correctCount = Expressions.numberTemplate(
            Long::class.javaObjectType,
            "sum(case when {0} then 1 else 0 end)",
            decisionResult.isCorrect,
        )

        return queryFactory
            .select(
                Projections.constructor(
                    AgentQueryRepository.AgentSummary::class.java,
                    agent.id,
                    agent.publicId,
                    agent.nickname,
                    agent.agentType,
                    agent.modelName,
                    agent.level,
                    agent.exp,
                    agent.dailySalary,
                    totalCount,
                    correctCount,
                ),
            ).from(agent)
            .leftJoin(briefing).on(briefing.agent.eq(agent))
            .leftJoin(decision).on(decision.briefing.eq(briefing))
            .leftJoin(decisionResult).on(decisionResult.decision.eq(decision))
            .where(agent.user.publicId.eq(userPublicId))
            .groupBy(
                agent.id,
                agent.publicId,
                agent.nickname,
                agent.agentType,
                agent.modelName,
                agent.level,
                agent.exp,
                agent.dailySalary,
            ).orderBy(agent.id.asc())
            .fetch()
    }

    override fun findAgentDetail(
        userPublicId: UUID,
        agentPublicId: UUID,
    ): AgentQueryRepository.AgentDetail? {
        val totalCount = decisionResult.id.countDistinct()
        val correctCount = Expressions.numberTemplate(
            Long::class.javaObjectType,
            "count(distinct case when {0} then {1} else null end)",
            decisionResult.isCorrect,
            decisionResult.id,
        )
        val contributedAp = Expressions.numberTemplate(
            Long::class.javaObjectType,
            "coalesce(sum({0}), 0)",
            apTransaction.amount,
        )

        return queryFactory
            .select(
                Projections.constructor(
                    AgentQueryRepository.AgentDetail::class.java,
                    agent.id,
                    agent.publicId,
                    agent.nickname,
                    agent.agentType,
                    agent.level,
                    agent.exp,
                    agent.modelName,
                    agent.description,
                    agent.dailySalary,
                    totalCount,
                    correctCount,
                    contributedAp,
                ),
            ).from(agent)
            .leftJoin(briefing).on(briefing.agent.eq(agent))
            .leftJoin(decision).on(decision.briefing.eq(briefing))
            .leftJoin(decisionResult).on(decisionResult.decision.eq(decision))
            .leftJoin(apTransaction).on(
                apTransaction.targetType.eq(ApTransactionTargetType.DECISION),
                apTransaction.targetId.eq(decision.id),
                apTransaction.reason.eq(ApTransactionReason.DECISION_WIN),
                apTransaction.amount.gt(0),
            ).where(
                agent.user.publicId.eq(userPublicId),
                agent.publicId.eq(agentPublicId),
            ).groupBy(
                agent.id,
                agent.publicId,
                agent.nickname,
                agent.agentType,
                agent.level,
                agent.exp,
                agent.modelName,
                agent.description,
                agent.dailySalary,
            ).fetchOne()
    }

    override fun findCompletedWorkDates(agentId: Long): List<LocalDate> =
        queryFactory
            .select(briefing.updatedAt)
            .from(briefing)
            .where(
                briefing.agent.id.eq(agentId),
                briefing.status.eq(BriefingStatus.COMPLETED),
            ).fetch()
            .map { it.toLocalDate() }
            .distinct()
}
