package com.brifo.server.stock.repository

import com.brifo.server.stock.entity.UserStock
import com.brifo.server.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface UserStockRepository :
    JpaRepository<UserStock, Long>,
    UserStockQueryRepository {
    fun findAllByUser(user: User): List<UserStock>

    fun countByUser(user: User): Long

    @Modifying
    @Query("delete from UserStock userStock where userStock.user = :user")
    fun deleteAllByUser(user: User): Int
}
