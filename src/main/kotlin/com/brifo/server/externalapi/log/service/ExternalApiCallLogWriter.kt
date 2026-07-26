package com.brifo.server.externalapi.log.service

import com.brifo.server.externalapi.log.entity.ExternalApiCallLog

interface ExternalApiCallLogWriter {
    fun save(externalApiCallLog: ExternalApiCallLog)
}
