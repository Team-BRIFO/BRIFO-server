package com.brifo.server.news.entity

import com.brifo.server.stock.entity.Stock
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
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "news")
class News private constructor(
    stock: Stock,
    source: NewsSource,
    sourceUrl: String,
    title: String,
    summary: String?,
    importance: BigDecimal?,
    dedupKey: String,
    publishedAt: LocalDateTime,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "newsIdGenerator")
    @SequenceGenerator(name = "newsIdGenerator", sequenceName = "news_id_seq", allocationSize = 50)
    @Column(name = "id", nullable = false, updatable = false)
    var id: Long? = null
        protected set

    @Column(name = "public_id", nullable = false, insertable = false, updatable = false)
    @Generated(event = [EventType.INSERT])
    var publicId: UUID? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_id", nullable = false)
    var stock: Stock = stock
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 30)
    var source: NewsSource = source
        protected set

    @Column(name = "source_url", nullable = false, columnDefinition = "TEXT")
    var sourceUrl: String = sourceUrl
        protected set

    @Column(name = "title", nullable = false, length = 500)
    var title: String = title
        protected set

    @Column(name = "summary", columnDefinition = "TEXT")
    var summary: String? = summary
        protected set

    @Column(name = "importance", precision = 3, scale = 2)
    var importance: BigDecimal? = importance
        protected set

    @Column(name = "dedup_key", nullable = false, length = 255)
    var dedupKey: String = dedupKey
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 20)
    var processingStatus: NewsProcessingStatus = NewsProcessingStatus.PENDING
        protected set

    @Column(name = "published_at", nullable = false)
    var publishedAt: LocalDateTime = publishedAt
        protected set

    @Column(name = "crawled_at", nullable = false, insertable = false, updatable = false)
    var crawledAt: LocalDateTime? = null
        protected set

    companion object {
        fun create(
            stock: Stock,
            source: NewsSource,
            sourceUrl: String,
            title: String,
            summary: String?,
            importance: BigDecimal?,
            dedupKey: String,
            publishedAt: LocalDateTime,
        ): News =
            News(
                stock = stock,
                source = source,
                sourceUrl = sourceUrl,
                title = title,
                summary = summary,
                importance = importance,
                dedupKey = dedupKey,
                publishedAt = publishedAt,
            )
    }
}
