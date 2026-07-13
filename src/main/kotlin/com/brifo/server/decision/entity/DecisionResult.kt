package com.brifo.server.decision.entity

import com.brifo.server.global.common.BaseEntity
import com.brifo.server.stock.entity.DailyStockPrice
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.Table

@Entity
@Table(name = "decision_results")
class DecisionResult private constructor(
    decision: Decision,
    dailyStockPrice: DailyStockPrice,
    isCorrect: Boolean,
) : BaseEntity() {
    @Id
    @Column(name = "decision_id", nullable = false, updatable = false)
    var decisionId: Long? = null
        protected set

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "decision_id", nullable = false, updatable = false)
    var decision: Decision = decision
        protected set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "daily_stock_price_id", nullable = false, updatable = false)
    var dailyStockPrice: DailyStockPrice = dailyStockPrice
        protected set

    @Column(name = "is_correct", nullable = false, updatable = false)
    var isCorrect: Boolean = isCorrect
        protected set

    companion object {
        fun create(
            decision: Decision,
            dailyStockPrice: DailyStockPrice,
            isCorrect: Boolean,
        ): DecisionResult {
            return DecisionResult(
                decision = decision,
                dailyStockPrice = dailyStockPrice,
                isCorrect = isCorrect,
            )
        }
    }
}
