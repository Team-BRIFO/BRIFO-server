package com.brifo.server.badge.entity

import com.brifo.server.global.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "badges")
class Badge private constructor(
    code: String,
    name: String,
    description: String?,
    ap: Int,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "badgeIdGenerator")
    @SequenceGenerator(name = "badgeIdGenerator", sequenceName = "badges_id_seq", allocationSize = 50)
    @Column(name = "id", nullable = false, updatable = false)
    var id: Long? = null
        protected set

    @Column(name = "public_id", nullable = false, insertable = false, updatable = false)
    var publicId: UUID? = null
        protected set

    @Column(name = "code", nullable = false, length = 40)
    var code: String = code
        protected set

    @Column(name = "name", nullable = false, length = 50)
    var name: String = name
        protected set

    @Column(name = "description", length = 255)
    var description: String? = description
        protected set

    @Column(name = "ap", nullable = false)
    var ap: Int = ap
        protected set

    companion object {
        fun create(
            code: String,
            name: String,
            description: String?,
            ap: Int,
        ): Badge {
            return Badge(
                code = code,
                name = name,
                description = description,
                ap = ap,
            )
        }
    }
}
