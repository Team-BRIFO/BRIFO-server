package com.brifo.server.ap.repository

import com.brifo.server.ap.entity.ApTransactionReason
import com.brifo.server.ap.entity.ApTransactionTargetType
import com.brifo.server.ap.entity.QApTransaction.Companion.apTransaction
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

@Repository
class ApTransactionQueryRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : ApTransactionQueryRepository {
    override fun sumBriefingSalaryBalance(briefingId: Long): Int =
        queryFactory
            .select(
                Expressions.numberTemplate(
                    Int::class.javaObjectType,
                    "coalesce(sum({0}), 0)",
                    apTransaction.amount,
                ),
            )
            .from(apTransaction)
            .where(
                apTransaction.targetType.eq(ApTransactionTargetType.BRIEFING),
                apTransaction.targetId.eq(briefingId),
                apTransaction.reason.`in`(
                    ApTransactionReason.SALARY,
                    ApTransactionReason.SALARY_REFUND,
                ),
            ).fetchOne() ?: 0
}
