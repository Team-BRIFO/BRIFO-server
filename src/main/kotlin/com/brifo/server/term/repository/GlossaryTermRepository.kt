package com.brifo.server.term.repository

import com.brifo.server.term.entity.GlossaryTerm
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface GlossaryTermRepository : JpaRepository<GlossaryTerm, Long> {
    fun findByPublicId(publicId: UUID): GlossaryTerm?
}
