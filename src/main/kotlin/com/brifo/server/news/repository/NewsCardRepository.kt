package com.brifo.server.news.repository

import com.brifo.server.news.entity.NewsCard
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface NewsCardRepository :
    JpaRepository<NewsCard, Long>,
    NewsCardQueryRepository {
    @EntityGraph(attributePaths = ["news", "news.stock"])
    fun findByPublicId(publicId: UUID): NewsCard?
}
