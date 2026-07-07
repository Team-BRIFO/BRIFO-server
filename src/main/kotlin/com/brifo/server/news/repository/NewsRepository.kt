package com.brifo.server.news.repository

import com.brifo.server.news.entity.News
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface NewsRepository : JpaRepository<News, Long> {
    fun findByPublicId(publicId: UUID): News?
}
