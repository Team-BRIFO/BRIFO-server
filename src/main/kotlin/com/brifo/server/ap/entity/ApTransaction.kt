package com.brifo.server.ap.entity

import com.brifo.server.global.common.BaseEntity
import com.brifo.server.user.entity.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.annotations.Generated
import org.hibernate.generator.EventType
import java.util.UUID

@Entity
@Table(name = "ap_transactions")
class ApTransaction private constructor(
    user: User,
    amount: Int,
    reason: ApTransactionReason,
    refType: ApTransactionRefType?,
    refId: Long?,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "apTransactionIdGenerator")
    @SequenceGenerator(name = "apTransactionIdGenerator", sequenceName = "ap_transactions_id_seq", allocationSize = 50)
    @Column(name = "id", nullable = false, updatable = false)
    var id: Long? = null
        protected set

    @Column(name = "public_id", nullable = false, insertable = false, updatable = false)
    @Generated(event = [EventType.INSERT])
    var publicId: UUID? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User = user
        protected set

    @Column(name = "amount", nullable = false)
    var amount: Int = amount
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 30)
    var reason: ApTransactionReason = reason
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "ref_type", length = 30)
    var refType: ApTransactionRefType? = refType
        protected set

    @Column(name = "ref_id")
    var refId: Long? = refId
        protected set

    companion object {
        fun create(
            user: User,
            amount: Int,
            reason: ApTransactionReason,
            refType: ApTransactionRefType? = null,
            refId: Long? = null,
        ): ApTransaction {
            require((refType == null) == (refId == null)) {
                "refType and refId must both be set or both be null"
            }

            return ApTransaction(
                user = user,
                amount = amount,
                reason = reason,
                refType = refType,
                refId = refId,
            )
        }
    }
}
