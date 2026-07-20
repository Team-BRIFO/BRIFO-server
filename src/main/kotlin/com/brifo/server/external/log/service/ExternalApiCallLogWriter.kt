package com.brifo.server.external.log.service

import com.brifo.server.external.log.entity.ExternalApiCallLog

interface ExternalApiCallLogWriter {
    fun save(externalApiCallLog: ExternalApiCallLog)
}
