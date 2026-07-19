package com.brifo.server.news.entity

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
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.generator.EventType
import org.hibernate.type.SqlTypes
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "news_cards")
class NewsCard private constructor(
    news: News,
    headline: String,
    points: List<String>,
    keywords: List<String>,
    importanceBadge: ImportanceBadge,
    displayDate: LocalDate,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "newsCardIdGenerator")
    @SequenceGenerator(name = "newsCardIdGenerator", sequenceName = "news_cards_id_seq", allocationSize = 50)
    @Column(name = "id", nullable = false, updatable = false)
    var id: Long? = null
        protected set

    @Column(name = "public_id", nullable = false, insertable = false, updatable = false)
    @Generated(event = [EventType.INSERT])
    var publicId: UUID? = null
        protected set

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "news_id", nullable = false)
    var news: News = news
        protected set

    @Column(name = "headline", nullable = false, length = 80)
    var headline: String = headline
        protected set

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "points", nullable = false, columnDefinition = "jsonb")
    var points: List<String> = points
        protected set

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "keywords", nullable = false, columnDefinition = "jsonb")
    var keywords: List<String> = keywords
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "importance_badge", nullable = false, length = 5)
    var importanceBadge: ImportanceBadge = importanceBadge
        protected set

    @Column(name = "display_date", nullable = false)
    var displayDate: LocalDate = displayDate
        protected set

    companion object {
        fun create(
            news: News,
            headline: String,
            points: List<String>,
            keywords: List<String>,
            importanceBadge: ImportanceBadge,
            displayDate: LocalDate,
        ): NewsCard {
            return NewsCard(
                news = news,
                headline = headline,
                points = points,
                keywords = keywords,
                importanceBadge = importanceBadge,
                displayDate = displayDate,
            )
        }
    }
}
