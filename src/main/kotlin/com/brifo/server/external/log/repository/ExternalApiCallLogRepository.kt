package com.brifo.server.external.log.repository

import com.brifo.server.external.log.entity.ExternalApiCallLog
import org.springframework.data.jpa.repository.JpaRepository

interface ExternalApiCallLogRepository : JpaRepository<ExternalApiCallLog, Long>
