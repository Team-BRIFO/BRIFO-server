package com.brifo.server.briefing.entity

import com.brifo.server.news.entity.NewsCard
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table

@Entity
@Table(name = "briefing_news_cards")
class BriefingNewsCard private constructor(
    briefing: Briefing,
    newsCard: NewsCard,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "briefingNewsCardIdGenerator")
    @SequenceGenerator(
        name = "briefingNewsCardIdGenerator",
        sequenceName = "briefing_news_cards_id_seq",
        allocationSize = 50,
    )
    @Column(name = "id", nullable = false, updatable = false)
    var id: Long? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "briefing_id", nullable = false, updatable = false)
    var briefing: Briefing = briefing
        protected set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false, updatable = false)
    var newsCard: NewsCard = newsCard
        protected set

    companion object {
        fun create(
            briefing: Briefing,
            newsCard: NewsCard,
        ): BriefingNewsCard {
            return BriefingNewsCard(
                briefing = briefing,
                newsCard = newsCard,
            )
        }
    }
}
