package com.brifo.server.ap.repository

import com.brifo.server.ap.entity.ApTransaction
import com.brifo.server.ap.entity.ApTransactionReason
import com.brifo.server.ap.entity.ApTransactionTargetType
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ApTransactionRepository : JpaRepository<ApTransaction, Long>, ApTransactionQueryRepository {
    fun findByPublicId(publicId: UUID): ApTransaction?

    fun findTopByUserPublicIdAndTargetTypeAndTargetIdAndReasonInOrderByIdDesc(
        userPublicId: UUID,
        targetType: ApTransactionTargetType,
        targetId: Long,
        reasons: Collection<ApTransactionReason>,
    ): ApTransaction?
}
