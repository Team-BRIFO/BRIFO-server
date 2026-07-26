package com.brifo.server.ap.repository

import com.brifo.server.ap.entity.AttendanceReward
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AttendanceRewardRepository : JpaRepository<AttendanceReward, Long> {
    fun findByIdAndUserPublicId(
        id: Long,
        userPublicId: UUID,
    ): AttendanceReward?
    fun findTopByUserIdOrderByCreatedAtDesc(userId: Long): AttendanceReward?
    fun findTopByUserIdOrderByCreatedAtDescIdDesc(userId: Long): AttendanceReward?
}
