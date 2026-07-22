package com.brifo.server.badge.repository

import com.brifo.server.badge.entity.Badge
import org.springframework.data.jpa.repository.JpaRepository

interface BadgeRepository : JpaRepository<Badge, Long>, BadgeQueryRepository {
    fun findByCode(code: String): Badge?
}
