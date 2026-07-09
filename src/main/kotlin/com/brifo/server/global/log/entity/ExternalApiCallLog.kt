package com.brifo.server.global.log.entity

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
import tools.jackson.databind.JsonNode
import java.time.LocalDateTime

@Entity
@Table(name = "external_api_call_logs")
class ExternalApiCallLog private constructor(
    apiName: String,
    provider: String?,
    requestPayload: JsonNode?,
    responsePayload: JsonNode?,
    status: ExternalApiCallStatus?,
    httpStatusCode: Int?,
    latencyMs: Int?,
) {
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

    @Column(name = "api_name", nullable = false, length = 100)
    var apiName: String = apiName
        protected set

    @Column(name = "provider", length = 100)
    var provider: String? = provider
        protected set

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_payload", columnDefinition = "jsonb")
    var requestPayload: JsonNode? = requestPayload
        protected set

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_payload", columnDefinition = "jsonb")
    var responsePayload: JsonNode? = responsePayload
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    var status: ExternalApiCallStatus? = status
        protected set

    @Column(name = "http_status_code")
    var httpStatusCode: Int? = httpStatusCode
        protected set

    @Column(name = "retry_count", nullable = false)
    var retryCount: Int = 0
        protected set

    @Column(name = "latency_ms")
    var latencyMs: Int? = latencyMs
        protected set

    @Column(name = "called_at", nullable = false, insertable = false, updatable = false)
    var calledAt: LocalDateTime? = null
        protected set

    fun increaseRetryCount() {
        retryCount += 1
    }

    companion object {
        fun create(
            apiName: String,
            provider: String?,
            requestPayload: JsonNode?,
            responsePayload: JsonNode?,
            status: ExternalApiCallStatus?,
            httpStatusCode: Int?,
            latencyMs: Int?,
        ): ExternalApiCallLog {
            return ExternalApiCallLog(
                apiName = apiName,
                provider = provider,
                requestPayload = requestPayload,
                responsePayload = responsePayload,
                status = status,
                httpStatusCode = httpStatusCode,
                latencyMs = latencyMs,
            )
        }
    }
}
