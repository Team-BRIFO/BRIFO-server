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
import org.hibernate.annotations.SQLDelete
import org.hibernate.annotations.SQLRestriction
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime

@Entity
@Table(name = "user_stocks")
@SQLDelete(sql = "UPDATE user_stocks SET deleted_at = now() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
class UserStock private constructor(
    user: User,
    stock: Stock,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "userStockIdGenerator")
    @SequenceGenerator(name = "userStockIdGenerator", sequenceName = "user_stocks_id_seq", allocationSize = 50)
    @Column(name = "id", nullable = false, updatable = false)
    var id: Long? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User = user
        protected set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_id", nullable = false)
    var stock: Stock = stock
        protected set

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime? = null
        protected set

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null
        protected set

    companion object {
        fun create(
            user: User,
            stock: Stock,
        ): UserStock =
            UserStock(
                user = user,
                stock = stock,
            )
    }
}
