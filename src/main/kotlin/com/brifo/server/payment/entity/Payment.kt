package com.brifo.server.payment.entity

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
import java.time.LocalDateTime
import java.util.UUID

/**
 * 포인트 충전 결제 1건.
 *
 * 토스페이먼츠는 결제창을 띄우기 전에 우리 서버가 먼저 orderId를 발급해야 하고(ready),
 * 결제가 끝나면 클라이언트가 돌려준 paymentKey로 다시 승인 요청(confirm)을 보내야 최종
 * 확정된다. 이 엔티티는 그 사이 상태(READY → DONE/FAILED)를 들고 있는다.
 */
@Entity
@Table(name = "payments")
class Payment private constructor(
    user: User,
    orderId: String,
    amount: Int,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "paymentIdGenerator")
    @SequenceGenerator(name = "paymentIdGenerator", sequenceName = "payments_id_seq", allocationSize = 50)
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

    @Column(name = "order_id", nullable = false, updatable = false, unique = true)
    var orderId: String = orderId
        protected set

    @Column(name = "amount", nullable = false, updatable = false)
    var amount: Int = amount
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: PaymentStatus = PaymentStatus.READY
        protected set

    @Column(name = "payment_key")
    var paymentKey: String? = null
        protected set

    @Column(name = "method")
    var method: String? = null
        protected set

    @Column(name = "failure_reason")
    var failureReason: String? = null
        protected set

    @Column(name = "confirmed_at")
    var confirmedAt: LocalDateTime? = null
        protected set

    fun markDone(
        paymentKey: String,
        method: String?,
        confirmedAt: LocalDateTime,
    ) {
        check(status == PaymentStatus.READY) { "이미 처리된 결제입니다: $status" }
        this.status = PaymentStatus.DONE
        this.paymentKey = paymentKey
        this.method = method
        this.confirmedAt = confirmedAt
    }

    fun markFailed(reason: String) {
        check(status == PaymentStatus.READY) { "이미 처리된 결제입니다: $status" }
        this.status = PaymentStatus.FAILED
        this.failureReason = reason
    }

    companion object {
        fun create(
            user: User,
            orderId: String,
            amount: Int,
        ): Payment = Payment(user, orderId, amount)
    }
}
