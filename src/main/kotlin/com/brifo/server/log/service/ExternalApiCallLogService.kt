package com.brifo.server.log.service

import com.brifo.server.log.entity.ExternalApiCallLog
import com.brifo.server.log.repository.ExternalApiCallLogRepository
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

@Service
class ExternalApiCallLogService(
    private val externalApiCallLogRepository: ExternalApiCallLogRepository,
) {
    private val log = LoggerFactory.getLogger(ExternalApiCallLogService::class.java)
    private val objectMapper = ObjectMapper()

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

    fun calculateDurationMs(startedAt: Instant): Long {
        return Duration.between(startedAt, Instant.now()).toMillis()
    }

    fun redactPayload(payload: Any?): JsonNode? {
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
    fun save(command: ExternalApiCallLogCommand) {
        runCatching {
            val externalApiCallLog = ExternalApiCallLog.create(
                provider = command.provider,
                apiName = command.apiName,
                status = command.status,
                userId = command.userId,
                stockId = command.stockId,
                newsId = command.newsId,
                briefingId = command.briefingId,
                idempotencyKey = command.idempotencyKey,
                requestPayloadRedacted = command.requestPayloadRedacted,
                responsePayloadRedacted = command.responsePayloadRedacted,
                responseStatusCode = command.responseStatusCode,
                errorMessage = command.errorMessage,
                retryCount = command.retryCount,
                durationMs = command.durationMs,
                totalTokens = command.totalTokens,
                estimatedCostKrw = command.estimatedCostKrw,
                requestedAt = command.requestedAt,
                respondedAt = command.respondedAt,
            )

            externalApiCallLogRepository.save(externalApiCallLog)
        }.onFailure { exception ->
            log.warn(
                "Failed to save external API call log. apiName={}, provider={}, status={}",
                command.apiName,
                command.provider,
                command.status,
                exception,
            )
        }
    }
}
