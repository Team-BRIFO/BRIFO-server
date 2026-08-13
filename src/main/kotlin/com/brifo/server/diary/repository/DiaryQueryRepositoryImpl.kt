package com.brifo.server.diary.repository

import com.brifo.server.ap.entity.ApTransactionTargetType
import com.brifo.server.ap.entity.QApTransaction.Companion.apTransaction
import com.brifo.server.briefing.entity.QBriefingNewsCard.Companion.briefingNewsCard
import com.brifo.server.decision.entity.QDecision.Companion.decision
import com.brifo.server.decision.entity.QDecisionResult.Companion.decisionResult
import com.brifo.server.diary.entity.QDiaryEntry.Companion.diaryEntry
import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
class DiaryQueryRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : DiaryQueryRepository {
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
                    apTransaction.amount,
                    decisionResult.isCorrect,
                ),
            ).from(diaryEntry)
            .join(diaryEntry.decision, decision)
            .join(decisionResult).on(decisionResult.decision.eq(decision))
            .join(briefingNewsCard).on(briefingNewsCard.briefing.eq(decision.briefing))
            .join(apTransaction)
            .on(
                apTransaction.targetType.eq(ApTransactionTargetType.DECISION),
                apTransaction.targetId.eq(decision.id),
            ).where(
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
                    apTransaction.amount,
                    decision.briefing.agent.publicId,
                    decision.briefing.agent.agentType,
                    decision.briefing.agent.nickname,
                    decision.briefing.publicId,
                    decision.briefing.direction,
                    decision.briefing.confidenceRate,
                    decisionResult.isCorrect,
                    decision.confidenceLevel,
                ),
            ).from(diaryEntry)
            .join(diaryEntry.decision, decision)
            .join(decisionResult).on(decisionResult.decision.eq(decision))
            .join(briefingNewsCard).on(briefingNewsCard.briefing.eq(decision.briefing))
            .join(apTransaction)
            .on(
                apTransaction.targetType.eq(ApTransactionTargetType.DECISION),
                apTransaction.targetId.eq(decision.id),
            )
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

    override fun findStatsRows(userPublicId: UUID): List<DiaryStatsRow> =
        queryFactory
            .select(
                QDiaryStatsRow(
                    diaryEntry.publicId,
                    decisionResult.createdAt,
                    decisionResult.isCorrect,
                    decision.direction,
                    decision.confidenceLevel,
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
