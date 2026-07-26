package com.brifo.server.notification.repository

import com.brifo.server.notification.dto.response.GetNotificationsResponse
import java.util.UUID

interface NotificationQueryRepository {
    fun findPageByUserPublicId(
        userPublicId: UUID,
        cursor: UUID?,
        limit: Int,
    ): List<GetNotificationsResponse.Item>
}
