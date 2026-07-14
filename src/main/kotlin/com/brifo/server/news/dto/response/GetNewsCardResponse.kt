package com.brifo.server.news.dto.response

import com.brifo.server.news.entity.ImportanceBadge
import com.brifo.server.news.entity.NewsSource
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class GetNewsCardResponse(
    val stock: Stock,
    val newsCard: NewsCard,
) {
    data class Stock(
        val stockId: UUID,
        val name: String,
        val sector: String,
        val price: BigDecimal,
        val changeRate: BigDecimal,
        val tradeDate: LocalDate,
    )

    data class NewsCard(
        val cardId: UUID,
        val source: NewsSource,
        val headline: String,
        val importanceBadge: ImportanceBadge,
        val publishedDate: LocalDate,
        val points: List<String>,
        val keywords: List<String>,
        val terms: List<Term>,
    )

    data class Term(
        val termId: UUID,
        val surface: String,
        val displayOrder: Int,
    )
}
