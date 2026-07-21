package com.brifo.server.news.repository

import com.brifo.server.term.entity.NewsCardTerm
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository

interface NewsCardTermQueryRepository : JpaRepository<NewsCardTerm, Long> {
    @EntityGraph(attributePaths = ["term"])
    fun findAllByNewsCardIdOrderByDisplayOrderAsc(newsCardId: Long): List<NewsCardTerm>
}
