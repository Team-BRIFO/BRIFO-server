package com.brifo.server.term.repository

import com.brifo.server.term.entity.UserLearnedTerm
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserLearnedTermRepository :
    JpaRepository<UserLearnedTerm, Long>,
    UserLearnedTermQueryRepository {
    fun existsByUserIdAndTermId(
        userId: Long,
        termId: Long,
    ): Boolean

    fun countByUserId(userId: Long): Long

    @Modifying
    @Query(
        value =
            """
            INSERT INTO user_learned_terms (user_id, term_id)
            VALUES (:userId, :termId)
            ON CONFLICT (user_id, term_id) DO NOTHING
            """,
        nativeQuery = true,
    )
    fun insertIfAbsent(
        @Param("userId") userId: Long,
        @Param("termId") termId: Long,
    ): Int
}
