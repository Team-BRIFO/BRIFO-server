package com.brifo.server.user.repository

import com.brifo.server.ap.entity.ApTransactionReason
import com.brifo.server.ap.entity.QApTransaction.Companion.apTransaction
import com.brifo.server.ap.entity.QAttendanceReward.Companion.attendanceReward
import com.brifo.server.decision.entity.QDecision.Companion.decision
import com.brifo.server.decision.entity.QDecisionResult.Companion.decisionResult
import com.brifo.server.term.entity.QUserLearnedTerm.Companion.userLearnedTerm
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class UserMyPageQueryRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : UserMyPageQueryRepository {
    override fun getMyPageStats(
        userId: Long,
        weekStart: LocalDateTime,
        now: LocalDateTime,
    ): UserMyPageStats {
        val latestAttendance =
            queryFactory
                .select(attendanceReward.consecutiveDays, attendanceReward.createdAt)
                .from(attendanceReward)
                .where(attendanceReward.user.id.eq(userId))
                .orderBy(attendanceReward.createdAt.desc(), attendanceReward.id.desc())
                .fetchFirst()

        return UserMyPageStats(
            thisWeekEarnedAp = getThisWeekEarnedAp(userId, weekStart, now),
            correctDecisionCount = getSettledDecisionCount(userId, isCorrect = true),
            settledDecisionCount = getSettledDecisionCount(userId),
            totalDecisionCount = getTotalDecisionCount(userId),
            latestConsecutiveDays = latestAttendance?.get(attendanceReward.consecutiveDays) ?: 0,
            latestAttendanceAt = latestAttendance?.get(attendanceReward.createdAt),
            learnedTermCount = getLearnedTermCount(userId),
        )
    }

    private fun getThisWeekEarnedAp(
        userId: Long,
        weekStart: LocalDateTime,
        now: LocalDateTime,
    ): Int =
        queryFactory
            .select(
                Expressions.numberTemplate(
                    Int::class.javaObjectType,
                    "coalesce(sum({0}), 0)",
                    apTransaction.amount,
                ),
            ).from(apTransaction)
            .where(
                apTransaction.user.id.eq(userId),
                apTransaction.amount.gt(0),
                apTransaction.reason.ne(ApTransactionReason.SALARY_REFUND),
                apTransaction.createdAt.goe(weekStart),
                apTransaction.createdAt.loe(now),
            ).fetchOne() ?: 0

    private fun getSettledDecisionCount(
        userId: Long,
        isCorrect: Boolean? = null,
    ): Long =
        queryFactory
            .select(decisionResult.count())
            .from(decisionResult)
            .where(
                decisionResult.decision.briefing.agent.user.id
                    .eq(userId),
                isCorrect?.let(decisionResult.isCorrect::eq),
            ).fetchOne() ?: 0L

    private fun getTotalDecisionCount(userId: Long): Long =
        queryFactory
            .select(decision.count())
            .from(decision)
            .where(
                decision.briefing.agent.user.id
                    .eq(userId),
            ).fetchOne() ?: 0L

    private fun getLearnedTermCount(userId: Long): Long =
        queryFactory
            .select(userLearnedTerm.count())
            .from(userLearnedTerm)
            .where(userLearnedTerm.user.id.eq(userId))
            .fetchOne() ?: 0L
}
