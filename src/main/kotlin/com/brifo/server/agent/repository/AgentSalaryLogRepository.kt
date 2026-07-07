package com.brifo.server.agent.repository

import com.brifo.server.agent.entity.AgentSalaryLog
import org.springframework.data.jpa.repository.JpaRepository

interface AgentSalaryLogRepository : JpaRepository<AgentSalaryLog, Long>
