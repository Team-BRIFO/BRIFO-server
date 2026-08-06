package com.brifo.server.notification.entity

import com.brifo.server.global.common.BaseEntity
import com.brifo.server.user.entity.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.annotations.Generated
import org.hibernate.generator.EventType
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "notifications")
class Notification private constructor(
    user: User,
    notificationType: NotificationType,
    title: String,
    body: String?,
    targetType: NotificationTargetType,
    targetPublicId: UUID?,
    eventDate: LocalDate?,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notificationIdGenerator")
    @SequenceGenerator(name = "notificationIdGenerator", sequenceName = "notifications_id_seq", allocationSize = 50)
    @Column(name = "id", nullable = false, updatable = false)
    var id: Long? = null
        protected set

    @Column(name = "public_id", nullable = false, insertable = false, updatable = false)
    @Generated(event = [EventType.INSERT])
    var publicId: UUID? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User = user
        protected set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_type_id", nullable = false)
    var notificationType: NotificationType = notificationType
        protected set

    @Column(name = "title", nullable = false, length = 100)
    var title: String = title
        protected set

    @Column(name = "body", length = 500)
    var body: String? = body
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    var targetType: NotificationTargetType = targetType
        protected set

    @Column(name = "target_public_id")
    var targetPublicId: UUID? = targetPublicId
        protected set

    @Column(name = "event_date")
    var eventDate: LocalDate? = eventDate
        protected set

    companion object {
        fun create(
            user: User,
            notificationType: NotificationType,
            title: String,
            body: String?,
            targetType: NotificationTargetType,
            targetPublicId: UUID? = null,
            eventDate: LocalDate? = null,
        ): Notification {
            val targetRequired = targetType in TARGET_REQUIRED_TYPES
            require(targetRequired == (targetPublicId != null)) {
                "targetPublicId presence is not valid for targetType $targetType"
            }

            return Notification(
                user = user,
                notificationType = notificationType,
                title = title,
                body = body,
                targetType = targetType,
                targetPublicId = targetPublicId,
                eventDate = eventDate,
            )
        }

        private val TARGET_REQUIRED_TYPES = setOf(
            NotificationTargetType.DECISION,
            NotificationTargetType.BRIEFING,
            NotificationTargetType.BADGE,
            NotificationTargetType.AGENT,
            NotificationTargetType.STOCK_BRIEFINGS,
            NotificationTargetType.NEWS_CARD_LIST,
        )
    }
}
