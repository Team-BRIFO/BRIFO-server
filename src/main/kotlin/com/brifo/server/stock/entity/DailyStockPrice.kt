package com.brifo.server.stock.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "daily_stock_prices")
class DailyStockPrice private constructor(
    stock: Stock,
    tradeDate: LocalDate,
    closePrice: BigDecimal,
    changeRate: BigDecimal,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "dailyStockPriceIdGenerator")
    @SequenceGenerator(
        name = "dailyStockPriceIdGenerator",
        sequenceName = "daily_stock_prices_id_seq",
        allocationSize = 1,
    )
    @Column(name = "id", nullable = false, updatable = false)
    var id: Long? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_id", nullable = false)
    var stock: Stock = stock
        protected set

    @Column(name = "trade_date", nullable = false)
    var tradeDate: LocalDate = tradeDate
        protected set

    @Column(name = "close_price", nullable = false, precision = 12, scale = 2)
    var closePrice: BigDecimal = closePrice
        protected set

    @Column(name = "change_rate", nullable = false, precision = 5, scale = 2)
    var changeRate: BigDecimal = changeRate
        protected set

    @Column(name = "fetched_at", nullable = false, insertable = false, updatable = false)
    var fetchedAt: LocalDateTime? = null
        protected set

    companion object {
        fun create(
            stock: Stock,
            tradeDate: LocalDate,
            closePrice: BigDecimal,
            changeRate: BigDecimal,
        ): DailyStockPrice {
            return DailyStockPrice(
                stock = stock,
                tradeDate = tradeDate,
                closePrice = closePrice,
                changeRate = changeRate,
            )
        }
    }
}
