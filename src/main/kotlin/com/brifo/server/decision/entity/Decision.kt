package com.brifo.server.decision.entity

import com.brifo.server.briefing.entity.Briefing
import com.brifo.server.global.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.annotations.Generated
import org.hibernate.generator.EventType
import java.util.UUID

@Entity
@Table(name = "decisions")
class Decision private constructor(
    briefing: Briefing,
    direction: DecisionDirection,
    allocatedAp: Int,
    allocationRatePercent: Short,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "decisionIdGenerator")
    @SequenceGenerator(name = "decisionIdGenerator", sequenceName = "decisions_id_seq", allocationSize = 50)
    @Column(name = "id", nullable = false, updatable = false)
    var id: Long? = null
        protected set

    @Column(name = "public_id", nullable = false, insertable = false, updatable = false)
    @Generated(event = [EventType.INSERT])
    var publicId: UUID? = null
        protected set

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "briefing_id", nullable = false)
    var briefing: Briefing = briefing
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 10)
    var direction: DecisionDirection = direction
        protected set

    @Column(name = "allocated_ap", nullable = false)
    var allocatedAp: Int = allocatedAp
        protected set

    /** 결정 등록 시점 잔액 대비 배분 비율(%). 1~40 범위. */
    @Column(name = "allocation_rate_percent", nullable = false)
    var allocationRatePercent: Short = allocationRatePercent
        protected set

    companion object {
        const val MAX_ALLOCATION_RATE_PERCENT = 40

        fun create(
            briefing: Briefing,
            direction: DecisionDirection,
            allocatedAp: Int,
            allocationRatePercent: Int,
        ): Decision {
            require(allocatedAp > 0) {
                "allocatedAp must be positive"
            }
            require(allocationRatePercent in 1..MAX_ALLOCATION_RATE_PERCENT) {
                "allocationRatePercent must be between 1 and $MAX_ALLOCATION_RATE_PERCENT"
            }

            return Decision(
                briefing = briefing,
                direction = direction,
                allocatedAp = allocatedAp,
                allocationRatePercent = allocationRatePercent.toShort(),
            )
        }
    }
}
