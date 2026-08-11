package com.brifo.server.stock.repository

import com.brifo.server.stock.entity.PendingUserStock
import com.brifo.server.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface PendingUserStockRepository : JpaRepository<PendingUserStock, Long> {
    fun findAllByUser(user: User): List<PendingUserStock>

    fun findAllByUserAndEffectiveAtLessThanEqual(
        user: User,
        effectiveAt: LocalDateTime,
    ): List<PendingUserStock>

    fun existsByEffectiveAtLessThanEqual(effectiveAt: LocalDateTime): Boolean

    @Query(
        "select distinct pending.user.id from PendingUserStock pending " +
            "where pending.effectiveAt <= :effectiveAt order by pending.user.id",
    )
    fun findDistinctUserIdsByEffectiveAtLessThanEqual(effectiveAt: LocalDateTime): List<Long>

    @Modifying
    @Query("delete from PendingUserStock pending where pending.user = :user")
    fun deleteAllByUser(user: User): Int

    @Modifying
    @Query(
        "delete from PendingUserStock pending " +
            "where pending.user = :user and pending.effectiveAt <= :effectiveAt",
    )
    fun deleteAllByUserAndEffectiveAtLessThanEqual(
        user: User,
        effectiveAt: LocalDateTime,
    ): Int
}
