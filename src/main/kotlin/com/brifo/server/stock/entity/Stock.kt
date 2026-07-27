package com.brifo.server.stock.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.annotations.Generated
import org.hibernate.generator.EventType
import java.util.UUID

@Entity
@Table(name = "stocks")
class Stock private constructor(
    code: String,
    name: String,
    sector: String,
    logoUrl: String?,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "stockIdGenerator")
    @SequenceGenerator(name = "stockIdGenerator", sequenceName = "stocks_id_seq", allocationSize = 50)
    @Column(name = "id", nullable = false, updatable = false)
    var id: Long? = null
        protected set

    @Column(name = "public_id", nullable = false, insertable = false, updatable = false)
    @Generated(event = [EventType.INSERT])
    var publicId: UUID? = null
        protected set

    @Column(name = "code", nullable = false, length = 10)
    var code: String = code
        protected set

    @Column(name = "name", nullable = false, length = 100)
    var name: String = name
        protected set

    @Column(name = "sector", nullable = false, length = 50)
    var sector: String = sector
        protected set

    @Column(name = "logo_url", columnDefinition = "TEXT")
    var logoUrl: String? = logoUrl
        protected set

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true
        protected set

    companion object {
        fun create(
            code: String,
            name: String,
            sector: String,
            logoUrl: String? = null,
        ): Stock {
            return Stock(
                code = code,
                name = name,
                sector = sector,
                logoUrl = logoUrl,
            )
        }
    }
}
