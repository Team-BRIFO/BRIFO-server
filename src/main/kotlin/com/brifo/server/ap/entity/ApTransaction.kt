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
    targetType: ApTransactionTargetType?,
    targetId: Long?,
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
    @Column(name = "target_type", length = 30)
    var targetType: ApTransactionTargetType? = targetType
        protected set

    @Column(name = "target_id")
    var targetId: Long? = targetId
        protected set

    companion object {
        fun salary(
            user: User,
            briefingId: Long,
            salaryCost: Int,
        ): ApTransaction =
            create(
                user = user,
                amount = -salaryCost,
                reason = ApTransactionReason.SALARY,
                targetType = ApTransactionTargetType.BRIEFING,
                targetId = briefingId,
            )

        fun salaryRefund(
            user: User,
            briefingId: Long,
            refundAmount: Int,
        ): ApTransaction =
            create(
                user = user,
                amount = refundAmount,
                reason = ApTransactionReason.SALARY_REFUND,
                targetType = ApTransactionTargetType.BRIEFING,
                targetId = briefingId,
            )

        fun create(
            user: User,
            amount: Int,
            reason: ApTransactionReason,
            targetType: ApTransactionTargetType? = null,
            targetId: Long? = null,
        ): ApTransaction {
            require((targetType == null) == (targetId == null)) {
                "targetType and targetId must both be set or both be null"
            }
            validateTarget(reason, targetType)
            validateAmountSign(reason, amount)

            return ApTransaction(
                user = user,
                amount = amount,
                reason = reason,
                targetType = targetType,
                targetId = targetId,
            )
        }

        private fun validateTarget(
            reason: ApTransactionReason,
            targetType: ApTransactionTargetType?,
        ) {
            val expectedTargetType = when (reason) {
                ApTransactionReason.ATTENDANCE -> ApTransactionTargetType.ATTENDANCE_REWARD
                ApTransactionReason.BADGE -> ApTransactionTargetType.USER_BADGE
                ApTransactionReason.DECISION_WIN,
                ApTransactionReason.DECISION_LOSE,
                ApTransactionReason.NEUTRAL_HIT,
                ApTransactionReason.NEUTRAL_MISS -> ApTransactionTargetType.DECISION
                ApTransactionReason.SALARY,
                ApTransactionReason.SALARY_REFUND -> ApTransactionTargetType.BRIEFING
                ApTransactionReason.TUTORIAL,
                ApTransactionReason.CREDIT_LOAN,
                ApTransactionReason.INITIAL_GRANT -> null
            }

            require(targetType == expectedTargetType) {
                "targetType $targetType is not valid for reason $reason"
            }
        }

        private fun validateAmountSign(
            reason: ApTransactionReason,
            amount: Int,
        ) {
            if (reason == ApTransactionReason.NEUTRAL_MISS) {
                require(amount == 0) { "amount must be zero for reason $reason" }
                return
            }

            val mustBeNegative = reason == ApTransactionReason.DECISION_LOSE ||
                reason == ApTransactionReason.SALARY

            require(if (mustBeNegative) amount < 0 else amount > 0) {
                "amount sign is not valid for reason $reason"
            }
        }
    }
}
