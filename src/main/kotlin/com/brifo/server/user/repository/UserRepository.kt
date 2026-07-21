package com.brifo.server.user.repository

import com.brifo.server.user.entity.OAuthProvider
import com.brifo.server.user.entity.User
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import java.util.UUID

interface UserRepository : JpaRepository<User, Long> {
    fun findByPublicId(publicId: UUID): User?

    fun findByProviderAndSocialId(
        provider: OAuthProvider,
        socialId: String,
    ): User?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findForUpdateByPublicId(publicId: UUID): User?
}
