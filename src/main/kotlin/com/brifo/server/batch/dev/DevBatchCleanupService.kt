package com.brifo.server.batch.dev

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
) {
    @Transactional
    fun cleanupForCollection(targetDate: LocalDate): DevBatchCleanupResult {
        val newsIds = queryRepository.findNewsIdsPublishedBetween(
            fromInclusive = targetDate.atStartOfDay(),
            toExclusive = targetDate.plusDays(1).atStartOfDay(),
        )
        val cleanup = cleanupNewsCards(newsIds)
        val deletedNews = jdbc.updateByIds("DELETE FROM news WHERE id IN (:ids)", newsIds)
        return cleanup.copy(news = deletedNews)
    }

    @Transactional
    fun cleanupForGeneration(targetDate: LocalDate): DevBatchCleanupResult {
        val displayDate = targetDate.plusDays(1)
        val newsIds = queryRepository.findGenerationNewsIds(
            targetDate = targetDate,
            displayDate = displayDate,
        )
        val cleanup = cleanupNewsCards(newsIds)
        return cleanup
    }

    @Transactional
    fun cleanupForSingleGeneration(newsId: Long): DevBatchCleanupResult {
        val cardIds = queryRepository.findCardIds(listOf(newsId))
        if (cardIds.isEmpty()) {
            jdbc.updateByIds(
                "UPDATE news SET processing_status = 'PENDING' WHERE id IN (:ids)",
                listOf(newsId),
            )
            return DevBatchCleanupResult()
        }

        val briefingIds = queryRepository.findBriefingIds(cardIds)
        val decisionIds = if (briefingIds.isEmpty()) emptyList() else queryRepository.findDecisionIds(briefingIds)

        deleteDecisionNotifications(decisionIds)
        deleteBriefingNotifications(briefingIds)
        deleteNewsCardNotifications(cardIds)

        jdbc.updateByIds("DELETE FROM diary_entries WHERE decision_id IN (:ids)", decisionIds)
        val settlements = deleteSettlementsAndRestoreRewards(decisionIds)
        val decisions = jdbc.updateByIds("DELETE FROM decisions WHERE id IN (:ids)", decisionIds)
        jdbc.updateByIds("DELETE FROM briefing_news_cards WHERE briefing_id IN (:ids)", briefingIds)
        val briefings = jdbc.updateByIds("DELETE FROM briefings WHERE id IN (:ids)", briefingIds)
        jdbc.updateByIds("DELETE FROM news_card_terms WHERE card_id IN (:ids)", cardIds)
        val cards = jdbc.updateByIds("DELETE FROM news_cards WHERE id IN (:ids)", cardIds)
        jdbc.updateByIds(
            "UPDATE news SET processing_status = 'PENDING' WHERE id IN (:ids)",
            listOf(newsId),
        )

        return DevBatchCleanupResult(
            newsCards = cards,
            briefings = briefings,
            decisions = decisions,
            settlements = settlements,
        )
    }

    @Transactional
    fun cleanupForSettlement(targetDate: LocalDate): DevBatchCleanupResult {
        val decisionIds = queryRepository.findSettlementDecisionIds(
            targetDate = targetDate,
        )
        val stockIds = if (decisionIds.isEmpty()) emptyList() else queryRepository.findStockIdsByDecisionIds(decisionIds)

        deleteDecisionNotifications(decisionIds)
        jdbc.updateByIds("DELETE FROM diary_entries WHERE decision_id IN (:ids)", decisionIds)
        val settlements = deleteSettlementsAndRestoreRewards(decisionIds)
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

        val cleanupCardIds = queryRepository.findCardIds(newsIds)
        if (cleanupCardIds.isEmpty()) return DevBatchCleanupResult()

        val linkedBriefingIds = queryRepository.findBriefingIds(cleanupCardIds)
        val cleanupNewsIds =
            jdbc.queryForList(
                "SELECT news_id FROM news_cards WHERE id IN (:ids)",
                ids(cleanupCardIds),
                Long::class.javaObjectType,
            )
        jdbc.updateByIds("DELETE FROM briefing_news_cards WHERE card_id IN (:ids)", cleanupCardIds)
        val cleanupBriefingIds =
            if (linkedBriefingIds.isEmpty()) emptyList()
            else queryRepository.findBriefingIdsWithoutCards(linkedBriefingIds)
        val decisionIds =
            if (cleanupBriefingIds.isEmpty()) emptyList()
            else queryRepository.findDecisionIds(cleanupBriefingIds)

        deleteDecisionNotifications(decisionIds)
        deleteBriefingNotifications(cleanupBriefingIds)
        deleteNewsCardNotifications(cleanupCardIds)

        jdbc.updateByIds("DELETE FROM diary_entries WHERE decision_id IN (:ids)", decisionIds)
        val settlements = deleteSettlementsAndRestoreRewards(decisionIds)
        val decisions = jdbc.updateByIds("DELETE FROM decisions WHERE id IN (:ids)", decisionIds)
        val briefings =
            if (cleanupBriefingIds.isEmpty()) {
                0
            } else {
                jdbc.update(
                    """
                    DELETE FROM briefings briefing
                    WHERE briefing.id IN (:ids)
                      AND NOT EXISTS (
                          SELECT 1 FROM briefing_news_cards link
                          WHERE link.briefing_id = briefing.id
                      )
                    """,
                    ids(cleanupBriefingIds),
                )
            }
        jdbc.updateByIds("DELETE FROM news_card_terms WHERE card_id IN (:ids)", cleanupCardIds)
        val cards = jdbc.updateByIds("DELETE FROM news_cards WHERE id IN (:ids)", cleanupCardIds)
        jdbc.updateByIds(
            "UPDATE news SET processing_status = 'PENDING' WHERE id IN (:ids)",
            cleanupNewsIds,
        )

        return DevBatchCleanupResult(
            newsCards = cards,
            briefings = briefings,
            decisions = decisions,
            settlements = settlements,
        )
    }

    private fun deleteSettlementsAndRestoreRewards(decisionIds: List<Long>): Int {
        if (decisionIds.isEmpty()) return 0

        jdbc.update(
            """
            UPDATE users user_account
            SET balance_ap = user_account.balance_ap - reward.total_amount
            FROM (
                SELECT user_id, SUM(amount) AS total_amount
                FROM ap_transactions
                WHERE target_type = 'DECISION'
                  AND target_id IN (:ids)
                GROUP BY user_id
            ) reward
            WHERE user_account.id = reward.user_id
            """,
            ids(decisionIds),
        )
        jdbc.updateByIds(
            "DELETE FROM ap_transactions WHERE target_type = 'DECISION' AND target_id IN (:ids)",
            decisionIds,
        )
        val settlements = jdbc.updateByIds("DELETE FROM decision_results WHERE decision_id IN (:ids)", decisionIds)

        jdbc.update(
            """
            UPDATE agents agent
            SET level = LEAST(10, 1 + experience.total_exp / 100),
                exp = CASE WHEN experience.total_exp >= 900 THEN 0 ELSE experience.total_exp % 100 END
            FROM (
                SELECT affected_agent.id,
                       COALESCE(SUM(
                           CASE
                               WHEN result.id IS NULL THEN 0
                               WHEN result.is_correct = FALSE THEN 10
                               WHEN decision.direction = 'NEUTRAL' THEN 20
                               ELSE 50
                           END
                       ), 0)::INTEGER AS total_exp
                FROM (
                    SELECT DISTINCT briefing.agent_id AS id
                    FROM decisions decision
                    JOIN briefings briefing ON briefing.id = decision.briefing_id
                    WHERE decision.id IN (:ids)
                ) affected_agent
                LEFT JOIN briefings briefing ON briefing.agent_id = affected_agent.id
                LEFT JOIN decisions decision ON decision.briefing_id = briefing.id
                LEFT JOIN decision_results result ON result.decision_id = decision.id
                GROUP BY affected_agent.id
            ) experience
            WHERE agent.id = experience.id
            """,
            ids(decisionIds),
        )

        val userBadgeIds = findInvalidSettlementBadgeIds(decisionIds)
        if (userBadgeIds.isNotEmpty()) {
            jdbc.update(
                """
                UPDATE users user_account
                SET balance_ap = user_account.balance_ap - reward.total_amount
                FROM (
                    SELECT user_id, SUM(amount) AS total_amount
                    FROM ap_transactions
                    WHERE target_type = 'USER_BADGE'
                      AND target_id IN (:ids)
                    GROUP BY user_id
                ) reward
                WHERE user_account.id = reward.user_id
                """,
                ids(userBadgeIds),
            )
            jdbc.update(
                """
                DELETE FROM notifications notification
                USING notification_types type, user_badges user_badge, badges badge
                WHERE notification.notification_type_id = type.id
                  AND type.code = 'BADGE_AWARDED'
                  AND notification.user_id = user_badge.user_id
                  AND notification.target_public_id = badge.public_id
                  AND user_badge.badge_id = badge.id
                  AND user_badge.id IN (:ids)
                """,
                ids(userBadgeIds),
            )
            jdbc.updateByIds(
                "DELETE FROM ap_transactions WHERE target_type = 'USER_BADGE' AND target_id IN (:ids)",
                userBadgeIds,
            )
            jdbc.updateByIds("DELETE FROM user_badges WHERE id IN (:ids)", userBadgeIds)
        }
        return settlements
    }

    private fun findInvalidSettlementBadgeIds(decisionIds: List<Long>): List<Long> =
        jdbc.queryForList(
            """
            WITH affected_users AS (
                SELECT DISTINCT agent.user_id
                FROM decisions decision
                JOIN briefings briefing ON briefing.id = decision.briefing_id
                JOIN agents agent ON agent.id = briefing.agent_id
                WHERE decision.id IN (:ids)
            ), settlement_stats AS (
                SELECT affected_user.user_id,
                       COUNT(result.id) FILTER (WHERE result.is_correct = TRUE) AS correct_count,
                       COUNT(result.id) FILTER (
                           WHERE result.is_correct = TRUE AND decision.direction = 'NEUTRAL'
                       ) AS neutral_hit_count,
                       COALESCE(BOOL_OR(
                           result.is_correct = TRUE AND decision.confidence_level = 5
                       ), FALSE) AS has_high_confidence_hit,
                       EXISTS (
                           SELECT 1 FROM agents user_agent
                           WHERE user_agent.user_id = affected_user.user_id AND user_agent.level >= 5
                       ) AS has_level_five_agent
                FROM affected_users affected_user
                LEFT JOIN agents agent ON agent.user_id = affected_user.user_id
                LEFT JOIN briefings briefing ON briefing.agent_id = agent.id
                LEFT JOIN decisions decision ON decision.briefing_id = briefing.id
                LEFT JOIN decision_results result ON result.decision_id = decision.id
                GROUP BY affected_user.user_id
            )
            SELECT user_badge.id
            FROM user_badges user_badge
            JOIN badges badge ON badge.id = user_badge.badge_id
            JOIN settlement_stats stats ON stats.user_id = user_badge.user_id
            WHERE badge.code IN ('B03', 'B04', 'B07', 'B08', 'B09', 'B10')
              AND NOT CASE badge.code
                  WHEN 'B03' THEN stats.correct_count >= 1
                  WHEN 'B04' THEN stats.has_high_confidence_hit
                  WHEN 'B07' THEN stats.correct_count >= 10
                  WHEN 'B08' THEN stats.correct_count >= 50
                  WHEN 'B09' THEN stats.neutral_hit_count >= 5
                  WHEN 'B10' THEN stats.has_level_five_agent
              END
            """,
            ids(decisionIds),
            Long::class.javaObjectType,
        )

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
