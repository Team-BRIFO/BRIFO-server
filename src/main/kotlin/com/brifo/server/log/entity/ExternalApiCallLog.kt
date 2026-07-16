package com.brifo.server.log.entity

import com.brifo.server.global.common.BaseEntity
import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "external_api_call_logs")
class ExternalApiCallLog private constructor(
    provider: String,
    apiName: String,
    status: ExternalApiCallStatus,
    userId: Long?,
    stockId: Long?,
    newsId: Long?,
    briefingId: Long?,
    idempotencyKey: String?,
    requestPayloadRedacted: JsonNode?,
    responsePayloadRedacted: JsonNode?,
    responseStatusCode: Int?,
    errorMessage: String?,
    retryCount: Int,
    durationMs: Long?,
    totalTokens: Int?,
    estimatedCostKrw: BigDecimal?,
    requestedAt: LocalDateTime,
    respondedAt: LocalDateTime?,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "externalApiCallLogIdGenerator")
    @SequenceGenerator(
        name = "externalApiCallLogIdGenerator",
        sequenceName = "external_api_call_logs_id_seq",
        allocationSize = 50,
    )
    @Column(name = "id", nullable = false, updatable = false)
    var id: Long? = null
        protected set

    @Column(name = "provider", nullable = false, length = 50)
    var provider: String = provider
        protected set

    @Column(name = "api_name", nullable = false, length = 100)
    var apiName: String = apiName
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    var status: ExternalApiCallStatus = status
        protected set

    @Column(name = "user_id")
    var userId: Long? = userId
        protected set

    @Column(name = "stock_id")
    var stockId: Long? = stockId
        protected set

    @Column(name = "news_id")
    var newsId: Long? = newsId
        protected set

    @Column(name = "briefing_id")
    var briefingId: Long? = briefingId
        protected set

    @Column(name = "idempotency_key", length = 150)
    var idempotencyKey: String? = idempotencyKey
        protected set

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_payload_redacted", columnDefinition = "jsonb")
    var requestPayloadRedacted: JsonNode? = requestPayloadRedacted
        protected set

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_payload_redacted", columnDefinition = "jsonb")
    var responsePayloadRedacted: JsonNode? = responsePayloadRedacted
        protected set

    @Column(name = "response_status_code")
    var responseStatusCode: Int? = responseStatusCode
        protected set

    @Column(name = "error_message", columnDefinition = "text")
    var errorMessage: String? = errorMessage
        protected set

    @Column(name = "retry_count", nullable = false)
    var retryCount: Int = retryCount
        protected set

    @Column(name = "duration_ms")
    var durationMs: Long? = durationMs
        protected set

    @Column(name = "total_tokens")
    var totalTokens: Int? = totalTokens
        protected set

    @Column(name = "estimated_cost_krw", precision = 12, scale = 4)
    var estimatedCostKrw: BigDecimal? = estimatedCostKrw
        protected set

    @Column(name = "requested_at", nullable = false)
    var requestedAt: LocalDateTime = requestedAt
        protected set

    @Column(name = "responded_at")
    var respondedAt: LocalDateTime? = respondedAt
        protected set

    companion object {
        fun create(
            provider: String,
            apiName: String,
            status: ExternalApiCallStatus,
            userId: Long? = null,
            stockId: Long? = null,
            newsId: Long? = null,
            briefingId: Long? = null,
            idempotencyKey: String? = null,
            requestPayloadRedacted: JsonNode? = null,
            responsePayloadRedacted: JsonNode? = null,
            responseStatusCode: Int? = null,
            errorMessage: String? = null,
            retryCount: Int = 0,
            durationMs: Long? = null,
            totalTokens: Int? = null,
            estimatedCostKrw: BigDecimal? = null,
            requestedAt: LocalDateTime,
            respondedAt: LocalDateTime? = null,
        ): ExternalApiCallLog {
            return ExternalApiCallLog(
                provider = provider,
                apiName = apiName,
                status = status,
                userId = userId,
                stockId = stockId,
                newsId = newsId,
                briefingId = briefingId,
                idempotencyKey = idempotencyKey,
                requestPayloadRedacted = requestPayloadRedacted,
                responsePayloadRedacted = responsePayloadRedacted,
                responseStatusCode = responseStatusCode,
                errorMessage = errorMessage,
                retryCount = retryCount,
                durationMs = durationMs,
                totalTokens = totalTokens,
                estimatedCostKrw = estimatedCostKrw,
                requestedAt = requestedAt,
                respondedAt = respondedAt,
            )
        }
    }
}
