package com.brifo.server.ap.repository

import com.brifo.server.ap.entity.AttendanceReward
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime
import java.util.UUID

interface AttendanceRewardRepository : JpaRepository<AttendanceReward, Long> {
    fun findByIdAndUserPublicId(
        id: Long,
        userPublicId: UUID,
    ): AttendanceReward?
    fun findTopByUserIdOrderByCreatedAtDesc(userId: Long): AttendanceReward?
    fun findTopByUserIdOrderByCreatedAtDescIdDesc(userId: Long): AttendanceReward?
    fun existsByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
        userId: Long,
        from: LocalDateTime,
        to: LocalDateTime,
    ): Boolean

    fun countByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
        userId: Long,
        from: LocalDateTime,
        to: LocalDateTime,
    ): Long
}
