package com.brifo.server.batch.dev

import com.brifo.server.batch.collection.CollectionRound
import org.springframework.batch.core.BatchStatus
import java.time.LocalDate

data class DevNewsCollectionBatchRequest(
    val targetDate: LocalDate,
    val collectionRound: CollectionRound,
)

data class DevDateBatchRequest(
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
