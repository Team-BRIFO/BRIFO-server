package com.brifo.server.briefing.repository

import com.brifo.server.briefing.entity.Briefing
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.UUID

interface BriefingRepository :
    JpaRepository<Briefing, Long>,
    BriefingQueryRepository {
    fun findByPublicId(publicId: UUID): Briefing?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findForUpdateByPublicId(publicId: UUID): Briefing?

    fun countByAgentUserPublicId(userPublicId: UUID): Long

    /** DecisionRepository.forceCreatedAtForGuestSeeding와 같은 이유로, 게스트 시딩 전용이다. */
    @Modifying
    @Query(value = "UPDATE briefings SET created_at = :createdAt WHERE id = :id", nativeQuery = true)
    fun forceCreatedAtForGuestSeeding(
        @Param("id") id: Long,
        @Param("createdAt") createdAt: LocalDateTime,
    )
}
