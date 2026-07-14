package com.brifo.server.log.service

import com.brifo.server.log.entity.ExternalApiCallLog
import com.brifo.server.log.entity.ExternalApiCallStatus
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

    fun calculateDurationMs(
        startedAt: Instant,
        endedAt: Instant = Instant.now(),
    ): Long {
        return Duration.between(startedAt, endedAt).toMillis()
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
            externalApiCallLogRepository.save(command.toEntity())
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

    // '가변 부분'만 StatusFields로 변경 (이후 아래에서 'statusFields.responsePayload, statusFields.responseStatusCode, statusFields.errorMessage' 처럼 사용 예정)
    private fun ExternalApiCallLogCommand.toEntity(): ExternalApiCallLog {
        val statusFields = when (status) {
            ExternalApiCallStatus.SUCCESS -> StatusFields(
                responsePayload = responsePayloadRedacted,
                responseStatusCode = responseStatusCode,
                errorMessage = null,
            )

            ExternalApiCallStatus.FAIL -> StatusFields(
                responsePayload = responsePayloadRedacted,
                responseStatusCode = responseStatusCode,
                errorMessage = errorMessage,
            )

            ExternalApiCallStatus.TIMEOUT -> StatusFields(
                responsePayload = null,
                responseStatusCode = null,
                errorMessage = errorMessage,
            )
        }

        // 위의 statusFields로 가변 부분 data 전달
        return ExternalApiCallLog.create(
            provider = provider,
            apiName = apiName,
            status = status,
            userId = userId,
            stockId = stockId,
            newsId = newsId,
            briefingId = briefingId,
            idempotencyKey = idempotencyKey,
            requestPayloadRedacted = requestPayloadRedacted,
            responsePayloadRedacted = statusFields.responsePayload,
            responseStatusCode = statusFields.responseStatusCode,
            errorMessage = statusFields.errorMessage,
            retryCount = retryCount,
            durationMs = durationMs,
            totalTokens = totalTokens,
            estimatedCostKrw = estimatedCostKrw,
            requestedAt = requestedAt,
            respondedAt = respondedAt,
        )
    }

    private data class StatusFields(
        val responsePayload: JsonNode?, // '외부 API'가 보내준 응답 본문
        val responseStatusCode: Int?, // '외부 서버' 응답 (우리 로그 상태 ExternalAPiCallStatus.TIMEOUT 아님 주의)
        val errorMessage: String?, // '우리 서버'가 기록할 오류 설명
    )
}
