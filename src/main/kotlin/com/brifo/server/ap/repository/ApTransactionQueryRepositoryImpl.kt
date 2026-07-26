package com.brifo.server.ap.repository

import com.brifo.server.ap.dto.response.GetApTransactionsResponse
import com.brifo.server.ap.entity.ApTransactionReason
import com.brifo.server.ap.entity.ApTransactionTargetType
import com.brifo.server.ap.entity.QApTransaction.Companion.apTransaction
import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

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

    override fun findPageByUserId(
        userId: Long,
        cursor: UUID?,
        limit: Int,
    ): List<GetApTransactionsResponse.Item> =
        queryFactory
            .select(
                Projections.constructor(
                    GetApTransactionsResponse.Item::class.java,
                    apTransaction.publicId,
                    apTransaction.reason,
                    apTransaction.amount,
                    apTransaction.createdAt,
                ),
            ).from(apTransaction)
            .where(
                apTransaction.user.id.eq(userId),
                cursor?.let { apTransaction.publicId.lt(it) },
            ).orderBy(apTransaction.publicId.desc())
            .limit(limit.toLong())
            .fetch()

    override fun findMonthlyAmountsByUserId(
        userId: Long,
        monthStart: LocalDateTime,
        nextMonthStart: LocalDateTime,
    ): GetApTransactionsResponse.MonthlyAmounts {
        val refund = com.brifo.server.ap.entity.QApTransaction("refund")
        val refundableSalary =
            apTransaction.reason.eq(ApTransactionReason.SALARY)
                .and(
                    JPAExpressions
                        .selectOne()
                        .from(refund)
                        .where(
                            refund.reason.eq(ApTransactionReason.SALARY_REFUND),
                            refund.targetId.eq(apTransaction.targetId),
                            refund.targetType.eq(apTransaction.targetType),
                        ).exists(),
                )
        val monthlyEarned =
            Expressions.numberTemplate(
                Int::class.javaObjectType,
                "coalesce(sum(case when {0} > 0 and {1} <> {2} then {0} else 0 end), 0)",
                apTransaction.amount,
                apTransaction.reason,
                ApTransactionReason.SALARY_REFUND,
            )
        val monthlyLost =
            Expressions.numberTemplate(
                Int::class.javaObjectType,
                "coalesce(sum(case when {0} < 0 and not ({1}) then -{0} else 0 end), 0)",
                apTransaction.amount,
                refundableSalary,
            )

        return requireNotNull(
            queryFactory
            .select(
                Projections.constructor(
                    GetApTransactionsResponse.MonthlyAmounts::class.java,
                    monthlyEarned,
                    monthlyLost,
                ),
            ).from(apTransaction)
            .where(
                apTransaction.user.id.eq(userId),
                apTransaction.createdAt.goe(monthStart),
                apTransaction.createdAt.lt(nextMonthStart),
            ).fetchOne(),
        )
    }
}
