package com.brifo.server.briefing.repository

import com.brifo.server.briefing.entity.Briefing
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface BriefingRepository : JpaRepository<Briefing, Long> {
    fun findByPublicId(publicId: UUID): Briefing?
}
