package com.brifo.server.diary.repository

import com.brifo.server.ap.entity.ApTransactionTargetType
import com.brifo.server.ap.entity.QApTransaction.Companion.apTransaction
import com.brifo.server.briefing.entity.QBriefingNewsCard.Companion.briefingNewsCard
import com.brifo.server.decision.entity.QDecision
import com.brifo.server.decision.entity.QDecision.Companion.decision
import com.brifo.server.decision.entity.QDecisionResult.Companion.decisionResult
import com.brifo.server.diary.entity.QDiaryEntry.Companion.diaryEntry
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
class DiaryQueryRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : DiaryQueryRepository {
    /**
     * 결정 1건에는 등록 시 배분금 차감(예: -30000)과 정산 결과(적중 시 +2배, 오답 시 0,
     * 관망 적중 시 원금 환급) 두 건의 AP 거래가 따로 남는다. 이 둘을 그냥 조인하면 행이
     * 2배로 늘어나므로(캘린더 상세 중복 표시 버그), 합산해 결정 1건당 순손익 하나로 계산한다.
     */
    private fun netApDeltaSubquery(target: QDecision) =
        Expressions.numberTemplate(
            Int::class.javaObjectType,
            "coalesce({0}, 0)",
            JPAExpressions
                .select(Expressions.numberTemplate(Int::class.javaObjectType, "sum({0})", apTransaction.amount))
                .from(apTransaction)
                .where(
                    apTransaction.targetType.eq(ApTransactionTargetType.DECISION),
                    apTransaction.targetId.eq(target.id),
                ),
        )
    override fun findDiaryPage(
        userPublicId: UUID,
        cursor: UUID?,
        limit: Long,
    ): List<DiaryListRow> {
        val predicate =
            BooleanBuilder(diaryEntry.decision.briefing.agent.user.publicId.eq(userPublicId))
                .and(cursor?.let(diaryEntry.publicId::lt))

        return queryFactory
            .select(
                QDiaryListRow(
                    diaryEntry.publicId,
                    briefingNewsCard.newsCard.news.stock.publicId,
                    briefingNewsCard.newsCard.news.stock.name,
                    decisionResult.dailyStockPrice.price,
                    decisionResult.dailyStockPrice.changeRate,
                    decisionResult.dailyStockPrice.tradeDate,
                    decisionResult.dailyStockPrice.stock.logoUrl,
                    decision.direction,
                    netApDeltaSubquery(decision),
                    decisionResult.isCorrect,
                ),
            ).from(diaryEntry)
            .join(diaryEntry.decision, decision)
            .join(decisionResult).on(decisionResult.decision.eq(decision))
            .join(briefingNewsCard).on(briefingNewsCard.briefing.eq(decision.briefing))
            .where(
                predicate,
                decisionResult.dailyStockPrice.stock.eq(briefingNewsCard.newsCard.news.stock),
            )
            .distinct()
            .orderBy(diaryEntry.publicId.desc())
            .limit(limit)
            .fetch()
    }

    override fun findDiaryDetail(
        userPublicId: UUID,
        diaryPublicId: UUID,
    ): DiaryDetailRow? =
        queryFactory
            .select(
                QDiaryDetailRow(
                    diaryEntry.publicId,
                    diaryEntry.shareImageUrl,
                    briefingNewsCard.newsCard.news.stock.publicId,
                    briefingNewsCard.newsCard.news.stock.name,
                    decisionResult.dailyStockPrice.changeRate,
                    decisionResult.dailyStockPrice.tradeDate,
                    netApDeltaSubquery(decision),
                    decision.briefing.agent.publicId,
                    decision.briefing.agent.agentType,
                    decision.briefing.agent.nickname,
                    decision.briefing.publicId,
                    decision.briefing.direction,
                    decision.briefing.confidenceRate,
                    decisionResult.isCorrect,
                    decision.allocationRatePercent,
                ),
            ).from(diaryEntry)
            .join(diaryEntry.decision, decision)
            .join(decisionResult).on(decisionResult.decision.eq(decision))
            .join(briefingNewsCard).on(briefingNewsCard.briefing.eq(decision.briefing))
            .where(
                diaryEntry.publicId.eq(diaryPublicId),
                decision.briefing.agent.user.publicId.eq(userPublicId),
            ).distinct()
            .fetchOne()

    override fun findCalendarRows(
        userPublicId: UUID,
        from: LocalDateTime,
        until: LocalDateTime,
    ): List<DiaryCalendarRow> =
        queryFactory
            .select(
                QDiaryCalendarRow(
                    decision.createdAt,
                    decision.direction,
                    decisionResult.isCorrect,
                ),
            ).from(diaryEntry)
            .join(diaryEntry.decision, decision)
            .join(decisionResult).on(decisionResult.decision.eq(decision))
            .where(
                decision.briefing.agent.user.publicId.eq(userPublicId),
                decision.createdAt.goe(from),
                decision.createdAt.lt(until),
            ).orderBy(decision.createdAt.asc(), diaryEntry.publicId.asc())
            .fetch()

    override fun findDayDetailRows(
        userPublicId: UUID,
        from: LocalDateTime,
        until: LocalDateTime,
    ): List<DiaryDayDetailRow> =
        queryFactory
            .select(
                QDiaryDayDetailRow(
                    diaryEntry.publicId,
                    decision.createdAt,
                    briefingNewsCard.newsCard.news.stock.publicId,
                    briefingNewsCard.newsCard.news.stock.name,
                    briefingNewsCard.newsCard.news.stock.logoUrl,
                    decisionResult.dailyStockPrice.changeRate,
                    decision.direction,
                    decision.allocationRatePercent,
                    decisionResult.isCorrect,
                    netApDeltaSubquery(decision),
                    decision.briefing.agent.publicId,
                    decision.briefing.agent.agentType,
                    decision.briefing.agent.nickname,
                ),
            ).from(diaryEntry)
            .join(diaryEntry.decision, decision)
            .join(decisionResult).on(decisionResult.decision.eq(decision))
            .join(briefingNewsCard).on(briefingNewsCard.briefing.eq(decision.briefing))
            .where(
                decision.briefing.agent.user.publicId.eq(userPublicId),
                decision.createdAt.goe(from),
                decision.createdAt.lt(until),
            ).distinct()
            .orderBy(decision.createdAt.asc(), diaryEntry.publicId.asc())
            .fetch()

    override fun findStatsRows(userPublicId: UUID): List<DiaryStatsRow> =
        queryFactory
            .select(
                QDiaryStatsRow(
                    diaryEntry.publicId,
                    decisionResult.createdAt,
                    decisionResult.isCorrect,
                    decision.direction,
                    decision.allocationRatePercent,
                    decision.briefing.agent.publicId,
                    briefingNewsCard.newsCard.news.stock.publicId,
                    briefingNewsCard.newsCard.news.stock.name,
                ),
            ).from(diaryEntry)
            .join(diaryEntry.decision, decision)
            .join(decisionResult).on(decisionResult.decision.eq(decision))
            .join(briefingNewsCard).on(briefingNewsCard.briefing.eq(decision.briefing))
            .where(decision.briefing.agent.user.publicId.eq(userPublicId))
            .distinct()
            .orderBy(decisionResult.createdAt.asc(), diaryEntry.publicId.asc())
            .fetch()
}
