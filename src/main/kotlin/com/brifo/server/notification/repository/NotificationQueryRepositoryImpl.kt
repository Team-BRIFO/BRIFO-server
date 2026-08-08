package com.brifo.server.notification.repository

import com.brifo.server.notification.dto.response.GetNotificationsResponse
import com.brifo.server.notification.entity.NotificationTargetType
import com.brifo.server.notification.entity.QNotification.Companion.notification
import com.brifo.server.notification.entity.QNotificationType.Companion.notificationType
import com.brifo.server.user.entity.QUser.Companion.user
import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Repository
class NotificationQueryRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : NotificationQueryRepository {
    override fun findPageByUserPublicId(
        userPublicId: UUID,
        cursor: UUID?,
        limit: Int,
    ): List<GetNotificationsResponse.NotificationItem> =
        queryFactory
            .select(
                Projections.constructor(
                    NotificationRow::class.java,
                    notification.publicId,
                    notificationType.code,
                    notification.title,
                    notification.body,
                    notification.createdAt,
                    notification.targetType,
                    notification.targetPublicId,
                ),
            ).from(notification)
            .join(notification.user, user)
            .join(notification.notificationType, notificationType)
            .where(
                user.publicId.eq(userPublicId),
                cursor?.let(notification.publicId::lt),
            ).orderBy(notification.publicId.desc())
            .limit(limit.toLong())
            .fetch()
            .map(NotificationRow::toResponseItem)

    override fun findNewsCardArrivals(
        userIds: Collection<Long>,
        code: String,
        targetType: NotificationTargetType,
        targetPublicIds: Collection<UUID>,
        eventDate: LocalDate,
    ): Set<NewsCardArrival> =
        queryFactory
            .select(
                Projections.constructor(
                    NewsCardArrival::class.java,
                    notification.user.id,
                    notification.targetPublicId,
                ),
            ).distinct()
            .from(notification)
            .where(
                notification.user.id.`in`(userIds),
                notification.notificationType.code.eq(code),
                notification.targetType.eq(targetType),
                notification.targetPublicId.`in`(targetPublicIds),
                notification.eventDate.eq(eventDate),
            ).fetch()
            .toSet()
}

data class NotificationRow(
    val notificationId: UUID,
    val code: String,
    val title: String,
    val body: String?,
    val createdAt: LocalDateTime,
    val targetType: NotificationTargetType,
    val targetId: UUID?,
) {
    fun toResponseItem(): GetNotificationsResponse.NotificationItem =
        GetNotificationsResponse.NotificationItem(
            notificationId = notificationId,
            code = code,
            title = title,
            body = body,
            createdAt = createdAt,
            target =
                GetNotificationsResponse.Target(
                    type = targetType,
                    targetId = targetId,
                ),
        )
}
