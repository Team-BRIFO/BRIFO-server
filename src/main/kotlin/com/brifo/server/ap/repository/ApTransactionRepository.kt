package com.brifo.server.ap.repository

import com.brifo.server.ap.entity.ApTransaction
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ApTransactionRepository : JpaRepository<ApTransaction, Long>, ApTransactionQueryRepository {
    fun findByPublicId(publicId: UUID): ApTransaction?
}
