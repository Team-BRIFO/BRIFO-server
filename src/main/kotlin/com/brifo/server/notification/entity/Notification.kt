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
import java.util.UUID

@Entity
@Table(name = "notifications")
class Notification private constructor(
    user: User,
    notificationType: NotificationType,
    title: String,
    body: String?,
    refType: NotificationRefType?,
    refPublicId: UUID?,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notificationIdGenerator")
    @SequenceGenerator(name = "notificationIdGenerator", sequenceName = "notifications_id_seq", allocationSize = 50)
    @Column(name = "id", nullable = false, updatable = false)
    var id: Long? = null
        protected set

    @Column(name = "public_id", nullable = false, insertable = false, updatable = false)
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
    @Column(name = "ref_type", length = 30)
    var refType: NotificationRefType? = refType
        protected set

    @Column(name = "ref_public_id")
    var refPublicId: UUID? = refPublicId
        protected set

    companion object {
        fun create(
            user: User,
            notificationType: NotificationType,
            title: String,
            body: String?,
            refType: NotificationRefType? = null,
            refPublicId: UUID? = null,
        ): Notification {
            return Notification(
                user = user,
                notificationType = notificationType,
                title = title,
                body = body,
                refType = refType,
                refPublicId = refPublicId,
            )
        }
    }
}
