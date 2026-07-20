package com.brifo.server.log.dto

import com.brifo.server.log.entity.ExternalApiCallStatus
import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDateTime

data class ExternalApiCallLogSaveData(
    val provider: String,
    val apiName: String,
    val status: ExternalApiCallStatus,
    val userId: Long? = null,
    val stockId: Long? = null,
    val newsId: Long? = null,
    val briefingId: Long? = null,
    val idempotencyKey: String? = null,
    val requestPayloadRedacted: JsonNode? = null,
    val responsePayloadRedacted: JsonNode? = null,
    val responseStatusCode: Int? = null,
    val errorMessage: String? = null,
    val retryCount: Int = 0,
    val durationMs: Long? = null,
    val requestedAt: LocalDateTime,
    val respondedAt: LocalDateTime? = null,
)
