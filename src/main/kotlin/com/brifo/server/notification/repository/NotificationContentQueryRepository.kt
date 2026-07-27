package com.brifo.server.notification.repository

import java.time.LocalDate
import java.util.UUID

interface NotificationContentQueryRepository {
    fun findDecisionResultContent(
        userPublicId: UUID,
        decisionPublicId: UUID,
    ): NotificationContentProjection.DecisionResult?

    fun findBriefingReadyContent(
        userPublicId: UUID,
        briefingPublicId: UUID,
    ): NotificationContentProjection.BriefingReady?

    fun findNewsCardContents(
        userPublicId: UUID,
        stockPublicId: UUID,
        displayDate: LocalDate,
    ): List<NotificationContentProjection.NewsCard>

    fun findAgentSalaryContents(
        userPublicId: UUID,
        stockPublicId: UUID,
        displayDate: LocalDate,
    ): List<NotificationContentProjection.AgentSalary>
}
