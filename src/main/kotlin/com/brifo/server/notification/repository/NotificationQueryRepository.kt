package com.brifo.server.notification.repository

import com.brifo.server.notification.dto.response.GetNotificationsResponse
import com.brifo.server.notification.entity.NotificationTargetType
import java.time.LocalDate
import java.util.UUID

interface NotificationQueryRepository {
    fun findPageByUserPublicId(
        userPublicId: UUID,
        cursor: UUID?,
        limit: Int,
    ): List<GetNotificationsResponse.NotificationItem>

    fun findNewsCardArrivals(
        userIds: Collection<Long>,
        code: String,
        targetType: NotificationTargetType,
        targetPublicIds: Collection<UUID>,
        eventDate: LocalDate,
    ): Set<NewsCardArrival>
}

data class NewsCardArrival(
    val userId: Long,
    val targetPublicId: UUID,
)
