package com.brifo.server.policy.repository

import com.brifo.server.policy.entity.UserPolicy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserPolicyRepository : JpaRepository<UserPolicy, Long> {
    @Query(
        """
        SELECT up.policy.id
        FROM UserPolicy up
        WHERE up.user.id = :userId
          AND up.revokedAt IS NULL
        """,
    )
    fun findActivePolicyIdsByUserId(@Param("userId") userId: Long): Set<Long>

    @Modifying
    @Query(
        value =
            """
            INSERT INTO user_policies (user_id, policy_id)
            VALUES (:userId, :policyId)
            ON CONFLICT (user_id, policy_id) WHERE revoked_at IS NULL DO NOTHING
            """,
        nativeQuery = true,
    )
    fun insertActiveIfAbsent(
        @Param("userId") userId: Long,
        @Param("policyId") policyId: Long,
    ): Int

    @Modifying
    @Query(
        value =
            """
            UPDATE user_policies
            SET revoked_at = CURRENT_TIMESTAMP
            WHERE user_id = :userId
              AND policy_id = :policyId
              AND revoked_at IS NULL
            """,
        nativeQuery = true,
    )
    fun revokeActive(
        @Param("userId") userId: Long,
        @Param("policyId") policyId: Long,
    ): Int
}
