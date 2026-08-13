package com.brifo.server.stock.repository

import com.brifo.server.stock.entity.Stock
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface StockRepository :
    JpaRepository<Stock, Long>,
    StockQueryRepository {
    fun findByPublicId(publicId: UUID): Stock?

    fun findByCode(code: String): Stock?

    fun findAllByPublicIdInAndIsActiveTrue(publicIds: Collection<UUID>): List<Stock>

    fun findAllByIsActiveTrueOrderByCode(): List<Stock>

    /**
     * 뉴스 수집 대상 종목. 누군가 관심종목으로 담았거나, 기본 워치리스트에 속한 활성 종목이다.
     *
     * 관심종목만으로 좁히면 `user_stocks`가 비었을 때 수집 대상이 0건이 되어
     * 뉴스·카드뉴스가 영원히 생성되지 않는다.
     */
    @Query(
        "select stock from Stock stock " +
            "where stock.isActive = true " +
            "and (exists (select 1 from UserStock userStock where userStock.stock = stock) " +
            "or stock.code in :defaultWatchlistCodes) " +
            "order by stock.code",
    )
    fun findAllCollectionTargets(
        @Param("defaultWatchlistCodes") defaultWatchlistCodes: Collection<String>,
    ): List<Stock>
}
