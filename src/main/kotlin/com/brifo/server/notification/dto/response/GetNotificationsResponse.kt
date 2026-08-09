package com.brifo.server.notification.dto.response

import com.brifo.server.global.common.CursorPage
import com.brifo.server.notification.entity.NotificationTargetType
import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime
import java.util.UUID

data class GetNotificationsResponse(
    val page: CursorPage<NotificationItem>,
) {
    data class NotificationItem(
        val notificationId: UUID,
        val code: String,
        val title: String,
        val body: String?,
        @field:JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss[.SSSSSS]")
        val createdAt: LocalDateTime,
        val target: Target,
    )

    data class Target(
        val type: NotificationTargetType,
        val targetId: UUID?,
    )
}
