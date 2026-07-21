package com.brifo.server.user.repository

import java.time.LocalDateTime

interface UserMyPageQueryRepository {
    fun getMyPageStats(
        userId: Long,
        weekStart: LocalDateTime,
        now: LocalDateTime,
    ): UserMyPageStats
}

data class UserMyPageStats(
    val thisWeekEarnedAp: Int,
    val correctDecisionCount: Long,
    val settledDecisionCount: Long,
    val totalDecisionCount: Long,
    val latestConsecutiveDays: Int,
    val latestAttendanceAt: LocalDateTime?,
    val learnedTermCount: Long,
)
