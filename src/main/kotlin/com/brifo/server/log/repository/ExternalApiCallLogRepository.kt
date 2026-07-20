package com.brifo.server.log.repository

import com.brifo.server.log.entity.ExternalApiCallLog
import org.springframework.data.jpa.repository.JpaRepository

interface ExternalApiCallLogRepository : JpaRepository<ExternalApiCallLog, Long>
