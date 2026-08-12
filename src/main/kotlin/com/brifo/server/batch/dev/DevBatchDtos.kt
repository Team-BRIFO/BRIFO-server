package com.brifo.server.batch.dev

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.springframework.batch.core.BatchStatus
import java.time.LocalDate
import java.util.UUID

data class DevDateBatchRequest(
    @field:NotBlank
    val password: String,
    @field:NotNull
    val targetDate: LocalDate,
)

data class DevSingleNewsCardGenerationRequest(
    @field:NotBlank
    val password: String,
    @field:NotNull
    val newsId: UUID,
)

data class DevSingleNewsCardGenerationResponse(
    val newsId: UUID,
    val newsCardId: UUID,
    val displayDate: LocalDate,
)

data class DevBatchRunResponse(
    val jobName: String,
    val jobInstanceId: Long,
    val jobExecutionId: Long,
    val status: BatchStatus,
)

data class DevBatchCleanupResult(
    val news: Int = 0,
    val newsCards: Int = 0,
    val briefings: Int = 0,
    val decisions: Int = 0,
    val settlements: Int = 0,
)
