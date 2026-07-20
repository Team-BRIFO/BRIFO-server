package com.brifo.server.briefing.repository

import com.brifo.server.briefing.entity.Briefing
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import java.util.UUID

interface BriefingRepository :
    JpaRepository<Briefing, Long>,
    BriefingQueryRepository {
    fun findByPublicId(publicId: UUID): Briefing?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findForUpdateByPublicId(publicId: UUID): Briefing?
}
