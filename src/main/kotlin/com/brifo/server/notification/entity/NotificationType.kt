package com.brifo.server.notification.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table

@Entity
@Table(name = "notification_types")
class NotificationType private constructor(
    code: String,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notificationTypeIdGenerator")
    @SequenceGenerator(
        name = "notificationTypeIdGenerator",
        sequenceName = "notification_types_id_seq",
        allocationSize = 1,
    )
    @Column(name = "id", nullable = false, updatable = false)
    var id: Long? = null
        protected set

    @Column(name = "code", nullable = false, length = 40)
    var code: String = code
        protected set

    companion object {
        fun create(code: String): NotificationType {
            return NotificationType(code = code)
        }
    }
}
