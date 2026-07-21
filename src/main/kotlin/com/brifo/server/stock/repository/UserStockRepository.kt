package com.brifo.server.stock.repository

import com.brifo.server.stock.entity.UserStock
import org.springframework.data.jpa.repository.JpaRepository

interface UserStockRepository :
    JpaRepository<UserStock, Long>,
    UserStockQueryRepository {
    fun findAllByUserId(userId: Long): List<UserStock>
}
