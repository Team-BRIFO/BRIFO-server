package com.brifo.server.diary.repository

import com.brifo.server.diary.entity.DiaryEntry
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface DiaryEntryRepository :
    JpaRepository<DiaryEntry, Long>,
    DiaryQueryRepository {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        SELECT diary
        FROM DiaryEntry diary
        JOIN diary.decision decision
        JOIN decision.briefing briefing
        JOIN briefing.agent agent
        JOIN agent.user user
        WHERE diary.publicId = :diaryPublicId
          AND user.publicId = :userPublicId
        """,
    )
    fun findOwnedByPublicIdForUpdate(
        @Param("userPublicId") userPublicId: UUID,
        @Param("diaryPublicId") diaryPublicId: UUID,
    ): DiaryEntry?
}
