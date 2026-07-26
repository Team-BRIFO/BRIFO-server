package com.brifo.server.notification.repository

import com.brifo.server.notification.entity.NotificationType
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationTypeRepository : JpaRepository<NotificationType, Long> {
    fun findByCode(code: String): NotificationType?
}
