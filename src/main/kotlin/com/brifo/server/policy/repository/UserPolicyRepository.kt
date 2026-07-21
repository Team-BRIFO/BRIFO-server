package com.brifo.server.policy.repository

import com.brifo.server.policy.entity.UserPolicy
import com.brifo.server.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserPolicyRepository : JpaRepository<UserPolicy, Long> {
    @Query(
        """
        select count(userPolicy)
        from UserPolicy userPolicy
        where userPolicy.user = :user
          and userPolicy.revokedAt is null
          and userPolicy.policy.isRequired = true
          and userPolicy.policy.isActive = true
        """,
    )
    fun countActiveRequiredAgreements(
        @Param("user") user: User,
    ): Long
}
