package com.brifo.server.decision.entity

import com.brifo.server.briefing.entity.Briefing
import com.brifo.server.global.common.BaseEntity
import com.brifo.server.news.entity.NewsCard
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
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "decisions")
class Decision private constructor(
    user: User,
    newsCard: NewsCard,
    briefing: Briefing,
    direction: DecisionDirection,
    confidence: Short,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "decisionIdGenerator")
    @SequenceGenerator(name = "decisionIdGenerator", sequenceName = "decisions_id_seq", allocationSize = 50)
    @Column(name = "id", nullable = false, updatable = false)
    var id: Long? = null
        protected set

    @Column(name = "public_id", nullable = false, insertable = false, updatable = false)
    var publicId: UUID? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User = user
        protected set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    var newsCard: NewsCard = newsCard
        protected set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "briefing_id", nullable = false)
    var briefing: Briefing = briefing
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 10)
    var direction: DecisionDirection = direction
        protected set

    @Column(name = "confidence", nullable = false)
    var confidence: Short = confidence
        protected set

    @Column(name = "is_correct")
    var isCorrect: Boolean? = null
        protected set

    @Column(name = "settled_at")
    var settledAt: LocalDateTime? = null
        protected set

    companion object {
        fun create(
            user: User,
            newsCard: NewsCard,
            briefing: Briefing,
            direction: DecisionDirection,
            confidence: Short,
        ): Decision {
            return Decision(
                user = user,
                newsCard = newsCard,
                briefing = briefing,
                direction = direction,
                confidence = confidence,
            )
        }
    }
}
