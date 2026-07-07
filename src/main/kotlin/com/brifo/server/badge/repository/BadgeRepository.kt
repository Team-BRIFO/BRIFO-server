package com.brifo.server.badge.repository

import com.brifo.server.badge.entity.Badge
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface BadgeRepository : JpaRepository<Badge, Long> {
    fun findByPublicId(publicId: UUID): Badge?
}
