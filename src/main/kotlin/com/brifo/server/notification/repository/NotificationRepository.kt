package com.brifo.server.notification.repository

import com.brifo.server.notification.entity.Notification
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface NotificationRepository : JpaRepository<Notification, Long> {
    fun findByPublicId(publicId: UUID): Notification?
}
