package com.brifo.server.stock.repository

import com.brifo.server.stock.entity.PendingUserStock
import com.brifo.server.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface PendingUserStockRepository : JpaRepository<PendingUserStock, Long> {
    fun findAllByUser(user: User): List<PendingUserStock>

    @Modifying
    @Query("delete from PendingUserStock pending where pending.user = :user")
    fun deleteAllByUser(user: User): Int
}
