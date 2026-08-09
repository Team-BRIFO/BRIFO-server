package com.brifo.server.batch.dev

import com.brifo.server.batch.collection.CollectionRound
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.springframework.batch.core.BatchStatus
import java.time.LocalDate

data class DevNewsCollectionBatchRequest(
    @field:NotBlank
    val password: String,
    @field:NotNull
    val targetDate: LocalDate,
    @field:NotNull
    val collectionRound: CollectionRound,
)

data class DevDateBatchRequest(
    @field:NotBlank
    val password: String,
    @field:NotNull
    val targetDate: LocalDate,
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
