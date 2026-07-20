package com.brifo.server.policy.entity

import com.brifo.server.global.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.annotations.Generated
import org.hibernate.generator.EventType
import org.springframework.data.annotation.LastModifiedDate
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "policies")
class Policy private constructor(
    title: String,
    content: String,
    isRequired: Boolean,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "policyIdGenerator")
    @SequenceGenerator(name = "policyIdGenerator", sequenceName = "policies_id_seq", allocationSize = 50)
    @Column(name = "id", nullable = false, updatable = false)
    var id: Long? = null
        protected set

    @Column(name = "public_id", nullable = false, insertable = false, updatable = false)
    @Generated(event = [EventType.INSERT])
    var publicId: UUID? = null
        protected set

    @Column(name = "title", nullable = false, length = 100)
    var title: String = title
        protected set

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    var content: String = content
        protected set

    @Column(name = "is_required", nullable = false)
    var isRequired: Boolean = isRequired
        protected set

    @Column(name = "version", nullable = false, precision = 5, scale = 2)
    var version: BigDecimal = BigDecimal("1.0")
        protected set

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true
        protected set

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime? = null
        protected set

    companion object {
        fun create(
            title: String,
            content: String,
            isRequired: Boolean,
        ): Policy {
            return Policy(
                title = title,
                content = content,
                isRequired = isRequired,
            )
        }
    }
}
