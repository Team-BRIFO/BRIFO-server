package com.brifo.server.term.repository

import com.brifo.server.term.entity.NewsCardTerm
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository

interface NewsCardTermRepository : JpaRepository<NewsCardTerm, Long> {
    @EntityGraph(attributePaths = ["term"])
    fun findAllByNewsCardIdOrderByDisplayOrderAsc(newsCardId: Long): List<NewsCardTerm>
}
