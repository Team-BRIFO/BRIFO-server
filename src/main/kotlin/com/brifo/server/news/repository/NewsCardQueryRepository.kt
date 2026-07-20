package com.brifo.server.news.repository

import com.brifo.server.news.entity.NewsCard
import java.time.LocalDate
import java.util.UUID

interface NewsCardQueryRepository {
    fun findAnalysisCards(
        stockPublicId: UUID,
        displayDate: LocalDate,
    ): List<NewsCard>
}
