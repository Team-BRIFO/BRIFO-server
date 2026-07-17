package com.brifo.server.log.service

import com.brifo.server.log.entity.ExternalApiCallLog

interface ExternalApiCallLogWriter {
    fun save(externalApiCallLog: ExternalApiCallLog)
}
