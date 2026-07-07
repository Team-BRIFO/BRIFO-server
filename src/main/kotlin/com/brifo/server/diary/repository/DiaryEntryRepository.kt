package com.brifo.server.diary.repository

import com.brifo.server.diary.entity.DiaryEntry
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface DiaryEntryRepository : JpaRepository<DiaryEntry, Long> {
    fun findByPublicId(publicId: UUID): DiaryEntry?
}
