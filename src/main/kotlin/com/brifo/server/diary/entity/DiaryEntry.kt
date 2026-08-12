package com.brifo.server.diary.entity

import com.brifo.server.decision.entity.Decision
import com.brifo.server.global.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.annotations.Generated
import org.hibernate.generator.EventType
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "diary_entries")
class DiaryEntry private constructor(
    decision: Decision,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "diaryEntryIdGenerator")
    @SequenceGenerator(name = "diaryEntryIdGenerator", sequenceName = "diary_entries_id_seq", allocationSize = 50)
    @Column(name = "id", nullable = false, updatable = false)
    var id: Long? = null
        protected set

    @Column(name = "public_id", nullable = false, insertable = false, updatable = false)
    @Generated(event = [EventType.INSERT])
    var publicId: UUID? = null
        protected set

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "decision_id", nullable = false)
    var decision: Decision = decision
        protected set

    @Column(name = "share_image_url", columnDefinition = "TEXT")
    var shareImageUrl: String? = null
        protected set

    @Column(name = "share_image_created_at")
    var shareImageCreatedAt: LocalDateTime? = null
        protected set

    fun attachShareImage(
        url: String,
        createdAt: LocalDateTime,
    ) {
        require(url.isNotBlank()) { "share image URL must not be blank" }
        shareImageUrl = url
        shareImageCreatedAt = createdAt
    }

    companion object {
        fun create(decision: Decision): DiaryEntry =
            DiaryEntry(
                decision = decision,
            )
    }
}
