package com.brifo.server.diary.repository

import com.brifo.server.diary.entity.DiaryEntry
import org.springframework.data.jpa.repository.JpaRepository

interface DiaryEntryRepository :
    JpaRepository<DiaryEntry, Long>,
    DiaryQueryRepository
