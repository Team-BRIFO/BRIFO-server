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
import org.hibernate.annotations.Generated
import org.hibernate.generator.EventType
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "agents")
class Agent private constructor(
    user: User,
    agentType: AgentType,
    modelName: String,
    nickname: String,
    description: String?,
    dailySalary: Int,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "agentIdGenerator")
    @SequenceGenerator(name = "agentIdGenerator", sequenceName = "agents_id_seq", allocationSize = 50)
    @Column(name = "id", nullable = false, updatable = false)
    var id: Long? = null
        protected set

    @Column(name = "public_id", nullable = false, insertable = false, updatable = false)
    @Generated(event = [EventType.INSERT])
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

    @Column(name = "nickname", nullable = false, length = 50)
    var nickname: String = nickname
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

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime? = null
        protected set

    fun addExperience(amount: Int): Boolean {
        require(amount >= 0) { "EXP amount must not be negative" }
        if (level >= MAX_LEVEL || amount == 0) return false

        val previousLevel = level
        exp += amount
        while (level < MAX_LEVEL && exp >= EXP_PER_LEVEL) {
            exp -= EXP_PER_LEVEL
            level++
        }
        if (level == MAX_LEVEL) exp = 0
        return level > previousLevel
    }

    companion object {
        private const val MAX_LEVEL = 10
        private const val EXP_PER_LEVEL = 100

        fun create(
            user: User,
            agentType: AgentType,
            modelName: String,
            nickname: String,
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
