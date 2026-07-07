package com.brifo.server.ap.repository

import com.brifo.server.ap.entity.AttendanceReward
import org.springframework.data.jpa.repository.JpaRepository

interface AttendanceRewardRepository : JpaRepository<AttendanceReward, Long>
