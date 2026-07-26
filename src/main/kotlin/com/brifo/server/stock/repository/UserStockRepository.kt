package com.brifo.server.stock.repository

import com.brifo.server.stock.entity.UserStock
import com.brifo.server.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserStockRepository :
    JpaRepository<UserStock, Long>,
    UserStockQueryRepository {
    fun findAllByUserId(userId: Long): List<UserStock>
      
    fun findAllByUser(user: User): List<UserStock>

    fun countByUser(user: User): Long
}
