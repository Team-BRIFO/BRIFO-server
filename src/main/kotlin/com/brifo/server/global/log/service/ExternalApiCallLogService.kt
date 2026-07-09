package com.brifo.server.global.log.service

import com.brifo.server.global.log.entity.ExternalApiCallLog
import com.brifo.server.global.log.entity.ExternalApiCallStatus
import com.brifo.server.global.log.repository.ExternalApiCallLogRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.ObjectNode
import java.time.Duration
import java.time.Instant

@Service
class ExternalApiCallLogService(
    private val externalApiCallLogRepository: ExternalApiCallLogRepository,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(ExternalApiCallLogService::class.java)

    private val sensitiveFieldNames = setOf(
        "authorization",
        "apiKey",
        "accessToken",
        "appSecret",
        "password",
        "newsContent",
    )

    fun startTimer(): Instant {
        return Instant.now()
    }

    fun calculateLatencyMs(startedAt: Instant): Int {
        return Duration.between(startedAt, Instant.now()).toMillis().toInt()
    }

    fun payloadOf(payload: Any?): JsonNode? {
        return payload
            ?.let { objectMapper.valueToTree<JsonNode>(it) }
            ?.maskSensitiveFields()
    }

    private fun JsonNode.maskSensitiveFields(): JsonNode {
        return when (this) {
            is ObjectNode -> {
                properties().forEach { (fieldName, childNode) ->
                    if (fieldName in sensitiveFieldNames) {
                        put(fieldName, "******")
                    } else {
                        set(fieldName, childNode.maskSensitiveFields())
                    }
                }
                this
            }

            is ArrayNode -> {
                for (index in 0 until size()) {
                    set(index, get(index).maskSensitiveFields())
                }
                this
            }

            else -> this
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun saveSuccess(
        apiName: String,
        provider: String?,
        requestPayload: JsonNode?,
        responsePayload: JsonNode?,
        httpStatusCode: Int?,
        latencyMs: Int?,
    ) {
        save(
            apiName = apiName,
            provider = provider,
            requestPayload = requestPayload,
            responsePayload = responsePayload,
            status = ExternalApiCallStatus.SUCCESS,
            httpStatusCode = httpStatusCode,
            latencyMs = latencyMs,
        )
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun saveFail(
        apiName: String,
        provider: String?,
        requestPayload: JsonNode?,
        responsePayload: JsonNode?,
        httpStatusCode: Int?,
        latencyMs: Int?,
    ) {
        save(
            apiName = apiName,
            provider = provider,
            requestPayload = requestPayload,
            responsePayload = responsePayload,
            status = ExternalApiCallStatus.FAIL,
            httpStatusCode = httpStatusCode,
            latencyMs = latencyMs,
        )
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun saveTimeout(
        apiName: String,
        provider: String?,
        requestPayload: JsonNode?,
        latencyMs: Int?,
    ) {
        save(
            apiName = apiName,
            provider = provider,
            requestPayload = requestPayload,
            responsePayload = null,
            status = ExternalApiCallStatus.TIMEOUT,
            httpStatusCode = null,
            latencyMs = latencyMs,
        )
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun increaseRetryCount(apiCallLog: ExternalApiCallLog) {
        runCatching {
            apiCallLog.increaseRetryCount()
            externalApiCallLogRepository.save(apiCallLog)
        }.onFailure { e ->
            log.warn("Failed to increase external API retry count. logId={}", apiCallLog.id, e)
        }
    }

    private fun save(
        apiName: String,
        provider: String?,
        requestPayload: JsonNode?,
        responsePayload: JsonNode?,
        status: ExternalApiCallStatus,
        httpStatusCode: Int?,
        latencyMs: Int?,
    ) {
        runCatching {
            val log = ExternalApiCallLog.create(
                apiName = apiName,
                provider = provider,
                requestPayload = requestPayload,
                responsePayload = responsePayload,
                status = status,
                httpStatusCode = httpStatusCode,
                latencyMs = latencyMs,
            )

            externalApiCallLogRepository.save(log)
        }.onFailure { e ->
            log.warn("Failed to save external API call log. apiName={}, provider={}, status={}", apiName, provider, status, e)
        }
    }
}
