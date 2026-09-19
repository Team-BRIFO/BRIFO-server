package com.brifo.server.news.repository

import com.brifo.server.news.entity.NewsCard
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface NewsCardRepository :
    JpaRepository<NewsCard, Long>,
    NewsCardQueryRepository {
    fun existsByNewsId(newsId: Long): Boolean
    fun findByNewsId(newsId: Long): NewsCard?
    fun findByPublicId(publicId: UUID): NewsCard?
}
