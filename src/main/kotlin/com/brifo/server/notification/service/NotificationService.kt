package com.brifo.server.notification.service

import com.brifo.server.global.common.CursorPage
import com.brifo.server.notification.dto.request.GetNotificationsRequest
import com.brifo.server.notification.dto.response.GetNotificationsResponse
import com.brifo.server.notification.repository.NotificationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
) {
    @Transactional(readOnly = true)
    fun getNotifications(
        userId: UUID,
        request: GetNotificationsRequest,
    ): GetNotificationsResponse {
        val notifications =
            notificationRepository.findPageByUserPublicId(
                userPublicId = userId,
                cursor = request.cursor,
                limit = request.size + 1,
            )
        val hasNext = notifications.size > request.size
        val items = notifications.take(request.size)

        return GetNotificationsResponse(
            page =
                CursorPage(
                    items = items,
                    nextCursor = if (hasNext) items.last().notificationId else null,
                    hasNext = hasNext,
                ),
        )
    }
}
