package com.brifo.server.externalapi.log.repository

import com.brifo.server.externalapi.log.entity.ExternalApiCallLog
import org.springframework.data.jpa.repository.JpaRepository

interface ExternalApiCallLogRepository : JpaRepository<ExternalApiCallLog, Long>
