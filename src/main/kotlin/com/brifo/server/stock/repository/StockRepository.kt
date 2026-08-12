package com.brifo.server.stock.repository

import com.brifo.server.stock.entity.Stock
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface StockRepository :
    JpaRepository<Stock, Long>,
    StockQueryRepository {
    fun findByPublicId(publicId: UUID): Stock?

    fun findByCode(code: String): Stock?

    fun findAllByPublicIdInAndIsActiveTrue(publicIds: Collection<UUID>): List<Stock>

    fun findAllByIsActiveTrueOrderByCode(): List<Stock>

    @Query(
        "select distinct userStock.stock from UserStock userStock " +
            "where userStock.stock.isActive = true order by userStock.stock.code",
    )
    fun findAllInterestedActiveOrderByCode(): List<Stock>
}
