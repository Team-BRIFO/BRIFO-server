package com.brifo.server.external.log.service

import com.brifo.server.external.log.entity.ExternalApiCallLog
import com.brifo.server.external.log.repository.ExternalApiCallLogRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class JpaExternalApiCallLogWriter(
    private val externalApiCallLogRepository: ExternalApiCallLogRepository,
) : ExternalApiCallLogWriter {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun save(externalApiCallLog: ExternalApiCallLog) {
        externalApiCallLogRepository.saveAndFlush(externalApiCallLog)
    }
}
