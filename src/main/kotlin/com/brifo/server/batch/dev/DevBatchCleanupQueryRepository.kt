package com.brifo.server.batch.dev

import com.brifo.server.briefing.entity.QBriefingNewsCard.Companion.briefingNewsCard
import com.brifo.server.decision.entity.QDecision.Companion.decision
import com.brifo.server.news.entity.QNews.Companion.news
import com.brifo.server.news.entity.QNewsCard.Companion.newsCard
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Repository
@Profile("dev")
@ConditionalOnProperty(prefix = "app.dev-auth", name = ["enabled"], havingValue = "true")
class DevBatchCleanupQueryRepository(
    private val queryFactory: JPAQueryFactory,
) {
    fun findNewsIdsPublishedBetween(
        fromInclusive: LocalDateTime,
        toInclusive: LocalDateTime,
    ): List<Long> =
        queryFactory
            .select(news.id)
            .from(news)
            .where(
                news.publishedAt.goe(fromInclusive),
                news.publishedAt.loe(toInclusive),
            ).fetch()

    fun findGenerationNewsIds(
        targetDate: LocalDate,
        displayDate: LocalDate,
    ): List<Long> =
        queryFactory
            .select(news.id)
            .distinct()
            .from(newsCard)
            .join(newsCard.news, news)
            .where(
                news.publishedAt.goe(targetDate.atStartOfDay()),
                news.publishedAt.lt(targetDate.plusDays(1).atStartOfDay()),
                newsCard.displayDate.eq(displayDate),
            ).fetch()

    fun findCardIds(newsIds: List<Long>): List<Long> =
        queryFactory
            .select(newsCard.id)
            .from(newsCard)
            .where(newsCard.news.id.`in`(newsIds))
            .fetch()

    fun findBriefingIds(cardIds: List<Long>): List<Long> =
        queryFactory
            .select(briefingNewsCard.briefing.id)
            .distinct()
            .from(briefingNewsCard)
            .where(briefingNewsCard.newsCard.id.`in`(cardIds))
            .fetch()

    fun findCardNotificationTargetIds(cardIds: List<Long>): List<UUID> =
        queryFactory
            .select(newsCard.news.stock.publicId)
            .distinct()
            .from(newsCard)
            .where(newsCard.id.`in`(cardIds))
            .fetch()

    fun findCardDisplayDates(cardIds: List<Long>): List<LocalDate> =
        queryFactory
            .select(newsCard.displayDate)
            .distinct()
            .from(newsCard)
            .where(newsCard.id.`in`(cardIds))
            .fetch()

    fun findDecisionIds(briefingIds: List<Long>): List<Long> =
        queryFactory
            .select(decision.id)
            .from(decision)
            .where(decision.briefing.id.`in`(briefingIds))
            .fetch()

    fun findSettlementDecisionIds(
        targetDate: LocalDate,
        cutoff: LocalDateTime,
    ): List<Long> =
        queryFactory
            .select(decision.id)
            .distinct()
            .from(decision)
            .join(decision.briefing.briefingNewsCards, briefingNewsCard)
            .where(
                briefingNewsCard.newsCard.displayDate.eq(targetDate),
                decision.createdAt.lt(cutoff),
            ).fetch()

    fun findStockIdsByDecisionIds(decisionIds: List<Long>): List<Long> =
        queryFactory
            .select(briefingNewsCard.newsCard.news.stock.id)
            .distinct()
            .from(decision)
            .join(decision.briefing.briefingNewsCards, briefingNewsCard)
            .where(decision.id.`in`(decisionIds))
            .fetch()
}
