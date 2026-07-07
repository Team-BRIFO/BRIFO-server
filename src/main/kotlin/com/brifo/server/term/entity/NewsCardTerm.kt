package com.brifo.server.term.entity

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
@Table(name = "news_card_terms")
class NewsCardTerm private constructor(
    newsCard: NewsCard,
    term: GlossaryTerm,
    surface: String?,
    displayOrder: Int,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "newsCardTermIdGenerator")
    @SequenceGenerator(name = "newsCardTermIdGenerator", sequenceName = "news_card_terms_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false, updatable = false)
    var id: Long? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    var newsCard: NewsCard = newsCard
        protected set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "term_id", nullable = false)
    var term: GlossaryTerm = term
        protected set

    @Column(name = "surface", length = 80)
    var surface: String? = surface
        protected set

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = displayOrder
        protected set

    companion object {
        fun create(
            newsCard: NewsCard,
            term: GlossaryTerm,
            surface: String?,
            displayOrder: Int,
        ): NewsCardTerm {
            return NewsCardTerm(
                newsCard = newsCard,
                term = term,
                surface = surface,
                displayOrder = displayOrder,
            )
        }
    }
}
