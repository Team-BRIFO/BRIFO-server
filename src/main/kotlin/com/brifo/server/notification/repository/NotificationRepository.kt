package com.brifo.server.notification.repository

import com.brifo.server.notification.entity.Notification
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationRepository :
    JpaRepository<Notification, Long>,
    NotificationContentQueryRepository
