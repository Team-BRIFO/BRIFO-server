package com.brifo.server.ap.repository

import com.brifo.server.ap.entity.ApTransaction
import com.brifo.server.ap.entity.ApTransactionReason
import org.springframework.data.jpa.repository.JpaRepository

interface ApTransactionRepository : JpaRepository<ApTransaction, Long>, ApTransactionQueryRepository {
    fun existsByUserIdAndReason(
        userId: Long,
        reason: ApTransactionReason,
    ): Boolean
}
