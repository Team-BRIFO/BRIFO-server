package com.brifo.server.agent.entity

import com.brifo.server.global.common.BaseEntity
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
import java.time.LocalDate

@Entity
@Table(name = "agent_salary_logs")
class AgentSalaryLog private constructor(
    agent: Agent,
    salaryAmount: Int,
    salaryDate: LocalDate,
    status: AgentSalaryLogStatus,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "agentSalaryLogIdGenerator")
    @SequenceGenerator(
        name = "agentSalaryLogIdGenerator",
        sequenceName = "agent_salary_logs_id_seq",
        allocationSize = 50,
    )
    @Column(name = "id", nullable = false, updatable = false)
    var id: Long? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_id", nullable = false)
    var agent: Agent = agent
        protected set

    @Column(name = "salary_amount", nullable = false)
    var salaryAmount: Int = salaryAmount
        protected set

    @Column(name = "salary_date", nullable = false)
    var salaryDate: LocalDate = salaryDate
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    var status: AgentSalaryLogStatus = status
        protected set

    companion object {
        fun create(
            agent: Agent,
            salaryAmount: Int,
            salaryDate: LocalDate,
            status: AgentSalaryLogStatus,
        ): AgentSalaryLog {
            return AgentSalaryLog(
                agent = agent,
                salaryAmount = salaryAmount,
                salaryDate = salaryDate,
                status = status,
            )
        }
    }
}
