package com.brifo.server.briefing.entity

import com.brifo.server.agent.entity.Agent
import com.brifo.server.global.common.BaseEntity
import com.brifo.server.news.entity.NewsCard
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
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "briefings")
class Briefing private constructor(
    newsCard: NewsCard,
    agent: Agent,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "briefingIdGenerator")
    @SequenceGenerator(name = "briefingIdGenerator", sequenceName = "briefings_id_seq", allocationSize = 50)
    @Column(name = "id", nullable = false, updatable = false)
    var id: Long? = null
        protected set

    @Column(name = "public_id", nullable = false, insertable = false, updatable = false)
    @Generated(event = [EventType.INSERT])
    var publicId: UUID? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    var newsCard: NewsCard = newsCard
        protected set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_id", nullable = false)
    var agent: Agent = agent
        protected set

    @Column(name = "content_text", columnDefinition = "TEXT")
    var contentText: String? = null
        protected set

    @Column(name = "headline", length = 200)
    var headline: String? = null
        protected set

    @Column(name = "one_liner", length = 200)
    var oneLiner: String? = null
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", length = 10)
    var direction: BriefingDirection? = null
        protected set

    @Column(name = "confidence_rate")
    var confidenceRate: Short? = null
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: BriefingStatus = BriefingStatus.PENDING
        protected set

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime? = null
        protected set

    fun startAnalysis() {
        require(status == BriefingStatus.PENDING) { "Only pending briefings can start analysis" }
        status = BriefingStatus.ANALYZING
    }

    fun complete(
        direction: BriefingDirection,
        confidenceRate: Short,
        contentText: String,
        oneLiner: String,
        headline: String? = null,
    ) {
        require(status == BriefingStatus.ANALYZING) { "Only analyzing briefings can be completed" }
        require(confidenceRate.toInt() in 0..100) {
            "confidenceRate must be between 0 and 100"
        }
        require(contentText.isNotBlank()) { "contentText must not be blank" }
        require(oneLiner.isNotBlank()) { "oneLiner must not be blank" }

        this.direction = direction
        this.confidenceRate = confidenceRate
        this.contentText = contentText
        this.oneLiner = oneLiner
        this.headline = headline
        status = BriefingStatus.COMPLETED
    }

    fun fail() {
        require(status == BriefingStatus.ANALYZING) { "Only analyzing briefings can fail" }
        status = BriefingStatus.FAILED
    }

    companion object {
        fun create(
            newsCard: NewsCard,
            agent: Agent,
        ): Briefing {
            return Briefing(
                newsCard = newsCard,
                agent = agent,
            )
        }
    }
}
