package com.brifo.server.briefing.entity

import com.brifo.server.agent.entity.Agent
import com.brifo.server.global.common.BaseEntity
import com.brifo.server.news.entity.NewsCard
import jakarta.persistence.CascadeType
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
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
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
    @JoinColumn(name = "agent_id", nullable = false)
    var agent: Agent = agent
        protected set

    @Column(name = "content_text", columnDefinition = "TEXT")
    var contentText: String? = null
        protected set

    @Column(name = "headline", length = 200)
    var headline: String? = null
        protected set

    @Column(name = "summary", columnDefinition = "TEXT")
    var summary: String? = null
        protected set

    @Column(name = "personal_comment", columnDefinition = "TEXT")
    var personalComment: String? = null
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

    @OneToMany(mappedBy = "briefing", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("id ASC")
    private val briefingNewsCards: MutableList<BriefingNewsCard> = mutableListOf()

    val newsCards: List<NewsCard>
        get() = briefingNewsCards.map(BriefingNewsCard::newsCard)

    fun startAnalysis() {
        require(status == BriefingStatus.PENDING) { "Only pending briefings can start analysis" }
        status = BriefingStatus.ANALYZING
    }

    fun retry(newsCards: List<NewsCard>) {
        require(status == BriefingStatus.FAILED) { "Only failed briefings can be retried" }
        validateNewsCards(newsCards)

        replaceNewsCards(newsCards)
        clearAnalysisResult()
        status = BriefingStatus.PENDING
    }

    private fun replaceNewsCards(newsCards: List<NewsCard>) {
        briefingNewsCards.removeAll { it.newsCard !in newsCards }
        val connectedNewsCards = briefingNewsCards.map(BriefingNewsCard::newsCard).toSet()
        briefingNewsCards += newsCards.filterNot(connectedNewsCards::contains).map { newsCard ->
            BriefingNewsCard.create(briefing = this, newsCard = newsCard)
        }
    }

    private fun clearAnalysisResult() {
        direction = null
        confidenceRate = null
        contentText = null
        oneLiner = null
        headline = null
        summary = null
        personalComment = null
    }

    fun complete(
        direction: BriefingDirection,
        confidenceRate: Short,
        contentText: String,
        oneLiner: String,
        headline: String,
        summary: String,
        personalComment: String?,
    ) {
        require(status == BriefingStatus.ANALYZING) { "Only analyzing briefings can be completed" }
        require(confidenceRate.toInt() in 0..100) {
            "confidenceRate must be between 0 and 100"
        }
        require(contentText.isNotBlank()) { "contentText must not be blank" }
        require(oneLiner.isNotBlank()) { "oneLiner must not be blank" }
        require(headline.isNotBlank()) { "headline must not be blank" }
        require(summary.isNotBlank()) { "summary must not be blank" }

        this.direction = direction
        this.confidenceRate = confidenceRate
        this.contentText = contentText
        this.oneLiner = oneLiner
        this.headline = headline
        this.summary = summary
        this.personalComment = personalComment
        status = BriefingStatus.COMPLETED
    }

    fun fail() {
        require(status == BriefingStatus.ANALYZING) { "Only analyzing briefings can fail" }
        status = BriefingStatus.FAILED
    }

    companion object {
        fun create(
            newsCards: List<NewsCard>,
            agent: Agent,
        ): Briefing {
            validateNewsCards(newsCards)

            return Briefing(agent = agent).also { briefing ->
                briefing.briefingNewsCards += newsCards.map { newsCard ->
                    BriefingNewsCard.create(
                        briefing = briefing,
                        newsCard = newsCard,
                    )
                }
            }
        }

        private fun validateNewsCards(newsCards: List<NewsCard>) {
            require(newsCards.isNotEmpty()) { "A briefing requires news cards" }
            require(newsCards.distinct().size == newsCards.size) {
                "A briefing requires distinct news cards"
            }

            val firstNewsCard = newsCards.first()
            require(newsCards.all { it.news.stock == firstNewsCard.news.stock }) {
                "All briefing news cards must belong to the same stock"
            }
            require(newsCards.all { it.displayDate == firstNewsCard.displayDate }) {
                "All briefing news cards must have the same display date"
            }
        }
    }
}
