package com.brifo.server.agent.entity

import com.brifo.server.global.common.BaseEntity
import com.brifo.server.user.entity.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "agents")
class Agent private constructor(
    user: User,
    agentType: AgentType,
    modelName: String,
    nickname: String?,
    description: String?,
    dailySalary: Int,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "agentIdGenerator")
    @SequenceGenerator(name = "agentIdGenerator", sequenceName = "agents_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false, updatable = false)
    var id: Long? = null
        protected set

    @Column(name = "public_id", nullable = false, insertable = false, updatable = false)
    var publicId: UUID? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User = user
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "agent_type", nullable = false, length = 10)
    var agentType: AgentType = agentType
        protected set

    @Column(name = "model_name", nullable = false, length = 100)
    var modelName: String = modelName
        protected set

    @Column(name = "nickname", length = 50)
    var nickname: String? = nickname
        protected set

    @Column(name = "description", length = 255)
    var description: String? = description
        protected set

    @Column(name = "level", nullable = false)
    var level: Int = 1
        protected set

    @Column(name = "exp", nullable = false)
    var exp: Int = 0
        protected set

    @Column(name = "daily_salary", nullable = false)
    var dailySalary: Int = dailySalary
        protected set

    @Column(name = "total_analyses", nullable = false)
    var totalAnalyses: Int = 0
        protected set

    @Column(name = "correct_analyses", nullable = false)
    var correctAnalyses: Int = 0
        protected set

    @Column(name = "last_work_date")
    var lastWorkDate: LocalDate? = null
        protected set

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true
        protected set

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime? = null
        protected set

    companion object {
        fun create(
            user: User,
            agentType: AgentType,
            modelName: String,
            nickname: String?,
            description: String?,
            dailySalary: Int,
        ): Agent {
            return Agent(
                user = user,
                agentType = agentType,
                modelName = modelName,
                nickname = nickname,
                description = description,
                dailySalary = dailySalary,
            )
        }
    }
}
