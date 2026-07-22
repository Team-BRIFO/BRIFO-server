package com.brifo.server.badge.repository

import com.brifo.server.badge.entity.UserBadge
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserBadgeRepository : JpaRepository<UserBadge, Long> {
    fun findTopByUserPublicIdOrderByIdDesc(userPublicId: UUID): UserBadge?
}
