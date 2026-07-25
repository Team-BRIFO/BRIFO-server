package com.brifo.server.stock.entity

import com.brifo.server.global.common.BaseEntity
import com.brifo.server.user.entity.User
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
import java.time.LocalDateTime

@Entity
@Table(name = "pending_user_stocks")
class PendingUserStock private constructor(
    user: User,
    stock: Stock,
    effectiveAt: LocalDateTime,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pendingUserStockIdGenerator")
    @SequenceGenerator(
        name = "pendingUserStockIdGenerator",
        sequenceName = "pending_user_stocks_id_seq",
        allocationSize = 50,
    )
    @Column(name = "id", nullable = false, updatable = false)
    var id: Long? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    var user: User = user
        protected set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_id", nullable = false, updatable = false)
    var stock: Stock = stock
        protected set

    @Column(name = "effective_at", nullable = false, updatable = false)
    var effectiveAt: LocalDateTime = effectiveAt
        protected set

    companion object {
        fun create(
            user: User,
            stock: Stock,
            effectiveAt: LocalDateTime,
        ): PendingUserStock =
            PendingUserStock(
                user = user,
                stock = stock,
                effectiveAt = effectiveAt,
            )
    }
}
