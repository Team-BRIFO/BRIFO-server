package com.brifo.server.global.log.repository

import com.brifo.server.global.log.entity.ExternalApiCallLog
import org.springframework.data.jpa.repository.JpaRepository

interface ExternalApiCallLogRepository : JpaRepository<ExternalApiCallLog, Long>
