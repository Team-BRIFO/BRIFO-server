package com.brifo.server.batch.dev

import com.brifo.server.batch.collection.CollectionRound
import com.brifo.server.batch.common.BusinessDateCalculator
import com.brifo.server.decision.DecisionMarketPolicy
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Profile("dev")
@ConditionalOnProperty(prefix = "app.dev-auth", name = ["enabled"], havingValue = "true")
class DevBatchCleanupService(
    private val jdbc: NamedParameterJdbcTemplate,
    private val queryRepository: DevBatchCleanupQueryRepository,
    private val businessDateCalculator: BusinessDateCalculator,
) {
    @Transactional
    fun cleanupForCollection(
        targetDate: LocalDate,
        round: CollectionRound,
    ): DevBatchCleanupResult {
        val newsIds = queryRepository.findNewsIdsPublishedBetween(
            fromInclusive = targetDate.atStartOfDay(),
            toInclusive = round.cutoffAt(targetDate),
        )
        val cleanup = cleanupNewsCards(newsIds)
        val deletedNews = jdbc.updateByIds("DELETE FROM news WHERE id IN (:ids)", newsIds)
        return cleanup.copy(news = deletedNews)
    }

    @Transactional
    fun cleanupForGeneration(targetDate: LocalDate): DevBatchCleanupResult {
        val displayDate = businessDateCalculator.nextBusinessDay(targetDate)
        val newsIds = queryRepository.findGenerationNewsIds(
            targetDate = targetDate,
            displayDate = displayDate,
        )
        val cleanup = cleanupNewsCards(newsIds)
        jdbc.updateByIds(
            "UPDATE news SET processing_status = 'PENDING' WHERE id IN (:ids)",
            newsIds,
        )
        return cleanup
    }

    @Transactional
    fun cleanupForSettlement(targetDate: LocalDate): DevBatchCleanupResult {
        val decisionIds = queryRepository.findSettlementDecisionIds(
            targetDate = targetDate,
            cutoff = DecisionMarketPolicy.settlementCutoff(targetDate),
        )
        val stockIds = if (decisionIds.isEmpty()) emptyList() else queryRepository.findStockIdsByDecisionIds(decisionIds)

        deleteDecisionNotifications(decisionIds)
        jdbc.updateByIds("DELETE FROM diary_entries WHERE decision_id IN (:ids)", decisionIds)
        val settlements = jdbc.updateByIds("DELETE FROM decision_results WHERE decision_id IN (:ids)", decisionIds)
        if (stockIds.isNotEmpty()) {
            jdbc.update(
                """
                DELETE FROM daily_stock_prices
                WHERE stock_id IN (:ids)
                  AND trade_date = :targetDate
                  AND is_closing = TRUE
                """,
                ids(stockIds).addValue("targetDate", targetDate),
            )
        }
        return DevBatchCleanupResult(settlements = settlements)
    }

    private fun cleanupNewsCards(newsIds: List<Long>): DevBatchCleanupResult {
        if (newsIds.isEmpty()) return DevBatchCleanupResult()

        val cardIds = queryRepository.findCardIds(newsIds)
        if (cardIds.isEmpty()) return DevBatchCleanupResult()

        val briefingIds = queryRepository.findBriefingIds(cardIds)
        val decisionIds = if (briefingIds.isEmpty()) emptyList() else queryRepository.findDecisionIds(briefingIds)

        deleteDecisionNotifications(decisionIds)
        deleteBriefingNotifications(briefingIds)
        deleteNewsCardNotifications(cardIds)

        jdbc.updateByIds("DELETE FROM diary_entries WHERE decision_id IN (:ids)", decisionIds)
        val settlements = jdbc.updateByIds("DELETE FROM decision_results WHERE decision_id IN (:ids)", decisionIds)
        val decisions = jdbc.updateByIds("DELETE FROM decisions WHERE id IN (:ids)", decisionIds)
        jdbc.updateByIds("DELETE FROM briefing_news_cards WHERE briefing_id IN (:ids)", briefingIds)
        val briefings = jdbc.updateByIds("DELETE FROM briefings WHERE id IN (:ids)", briefingIds)
        jdbc.updateByIds("DELETE FROM news_card_terms WHERE card_id IN (:ids)", cardIds)
        val cards = jdbc.updateByIds("DELETE FROM news_cards WHERE id IN (:ids)", cardIds)

        return DevBatchCleanupResult(
            newsCards = cards,
            briefings = briefings,
            decisions = decisions,
            settlements = settlements,
        )
    }

    private fun deleteDecisionNotifications(decisionIds: List<Long>) {
        if (decisionIds.isEmpty()) return
        jdbc.update(
            """
            DELETE FROM notifications notification
            USING notification_types type, decisions decision
            WHERE notification.notification_type_id = type.id
              AND type.code = 'DECISION_RESULT'
              AND notification.target_public_id = decision.public_id
              AND decision.id IN (:ids)
            """,
            ids(decisionIds),
        )
    }

    private fun deleteBriefingNotifications(briefingIds: List<Long>) {
        if (briefingIds.isEmpty()) return
        jdbc.update(
            """
            DELETE FROM notifications notification
            USING notification_types type, briefings briefing
            WHERE notification.notification_type_id = type.id
              AND type.code = 'BRIEFING_READY'
              AND notification.target_public_id = briefing.public_id
              AND briefing.id IN (:ids)
            """,
            ids(briefingIds),
        )
    }

    private fun deleteNewsCardNotifications(cardIds: List<Long>) {
        if (cardIds.isEmpty()) return
        val targetIds = queryRepository.findCardNotificationTargetIds(cardIds)
        val displayDates = queryRepository.findCardDisplayDates(cardIds)
        jdbc.update(
            """
            DELETE FROM notifications notification
            USING notification_types type
            WHERE notification.notification_type_id = type.id
              AND type.code = 'NEWS_CARD_ARRIVED'
              AND notification.target_public_id IN (:targetIds)
              AND notification.event_date IN (:displayDates)
            """,
            MapSqlParameterSource()
                .addValue("targetIds", targetIds)
                .addValue("displayDates", displayDates),
        )
    }

    private fun NamedParameterJdbcTemplate.updateByIds(
        sql: String,
        values: List<Long>,
    ): Int = if (values.isEmpty()) 0 else update(sql, ids(values))

    private fun ids(values: List<Long>): MapSqlParameterSource =
        MapSqlParameterSource("ids", values)
}
