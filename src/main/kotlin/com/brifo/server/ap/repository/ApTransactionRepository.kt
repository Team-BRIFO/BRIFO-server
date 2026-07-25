package com.brifo.server.ap.repository

import com.brifo.server.ap.entity.ApTransaction
import com.brifo.server.ap.entity.ApTransactionReason
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime
import java.util.UUID

interface ApTransactionRepository : JpaRepository<ApTransaction, Long>, ApTransactionQueryRepository {
    fun findByPublicId(publicId: UUID): ApTransaction?

    @Query(
        """
        select coalesce(sum(apTransaction.amount), 0)
        from ApTransaction apTransaction
        where apTransaction.user.id = :userId
          and apTransaction.amount > 0
          and apTransaction.reason <> :excludedReason
          and apTransaction.createdAt >= :from
          and apTransaction.createdAt <= :to
        """,
    )
    fun sumEarnedAmount(
        userId: Long,
        excludedReason: ApTransactionReason,
        from: LocalDateTime,
        to: LocalDateTime,
    ): Long
}
