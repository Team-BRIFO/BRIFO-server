package com.brifo.server.policy.repository

import com.brifo.server.policy.entity.Policy
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PolicyRepository : JpaRepository<Policy, Long>, PolicyQueryRepository {
    fun findByPublicId(publicId: UUID): Policy?

    fun countByIsRequiredTrueAndIsActiveTrue(): Long

    fun findByPublicIdAndIsActiveTrue(publicId: UUID): Policy?

    fun findAllByPublicIdInAndIsActiveTrue(publicIds: Collection<UUID>): List<Policy>

    fun findAllByIsActiveTrueAndIsRequiredTrue(): List<Policy>
}
