package com.brifo.server.decision.repository

import com.brifo.server.decision.entity.Decision
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.UUID

interface DecisionRepository :
    JpaRepository<Decision, Long>,
    DecisionQueryRepository {
    fun existsByPublicIdAndBriefingAgentUserPublicId(
        publicId: UUID,
        userPublicId: UUID,
    ): Boolean

    fun findByPublicId(publicId: UUID): Decision?

    fun countByBriefingAgentUserId(userId: Long): Long

    /**
     * `created_at`은 `@CreatedDate`+`updatable = false`라 일반 저장으로는 값을 바꿀 수 없다.
     * 게스트 목데이터 시딩이 과거 날짜로 결정일기를 채울 때, 캘린더 조회가 이 컬럼만으로
     * 날짜를 판별하기 때문에 이 값을 직접 되돌려 써야 한다. 다른 용도로 쓰지 않는다.
     */
    @Modifying
    @Query(value = "UPDATE decisions SET created_at = :createdAt WHERE id = :id", nativeQuery = true)
    fun forceCreatedAtForGuestSeeding(
        @Param("id") id: Long,
        @Param("createdAt") createdAt: LocalDateTime,
    )
}
