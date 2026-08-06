package com.brifo.server.batch.dev

import com.brifo.server.auth.dev.DevAuthProperties
import com.brifo.server.batch.collection.NewsCollectionJobConfiguration
import com.brifo.server.batch.common.BatchJobParameters
import com.brifo.server.batch.generation.NewsCardGenerationJobConfiguration
import com.brifo.server.batch.settlement.DecisionSettlementJobConfiguration
import com.brifo.server.global.code.ErrorCode
import com.brifo.server.global.exception.BusinessException
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.JobExecution
import org.springframework.batch.core.launch.JobOperator
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

@Service
@Profile("dev")
@ConditionalOnProperty(prefix = "app.dev-auth", name = ["enabled"], havingValue = "true")
class DevBatchService(
    private val properties: DevAuthProperties,
    private val cleanupService: DevBatchCleanupService,
    private val jobOperator: JobOperator,
    @Qualifier(NewsCollectionJobConfiguration.JOB_NAME)
    private val newsCollectionJob: Job,
    @Qualifier(NewsCardGenerationJobConfiguration.JOB_NAME)
    private val newsCardGenerationJob: Job,
    @Qualifier(DecisionSettlementJobConfiguration.JOB_NAME)
    private val decisionSettlementJob: Job,
) {
    @Synchronized
    fun rerunNewsCollection(
        request: DevNewsCollectionBatchRequest,
    ): DevBatchRunResponse {
        verifyPassword(request.password)
        cleanupService.cleanupForCollection(request.targetDate, request.collectionRound)
        val execution = jobOperator.start(
            newsCollectionJob,
            BatchJobParameters.forDevCollection(request.targetDate, request.collectionRound.name),
        )
        return execution.toResponse()
    }

    @Synchronized
    fun rerunNewsCardGeneration(
        request: DevDateBatchRequest,
    ): DevBatchRunResponse {
        verifyPassword(request.password)
        cleanupService.cleanupForGeneration(request.targetDate)
        val execution = jobOperator.start(newsCardGenerationJob, BatchJobParameters.forDevDate(request.targetDate))
        return execution.toResponse()
    }

    @Synchronized
    fun rerunDecisionSettlement(
        request: DevDateBatchRequest,
    ): DevBatchRunResponse {
        verifyPassword(request.password)
        cleanupService.cleanupForSettlement(request.targetDate)
        val execution = jobOperator.start(decisionSettlementJob, BatchJobParameters.forDevDate(request.targetDate))
        return execution.toResponse()
    }

    private fun verifyPassword(actual: String) {
        val matches =
            MessageDigest.isEqual(
                properties.password.toByteArray(StandardCharsets.UTF_8),
                actual.toByteArray(StandardCharsets.UTF_8),
            )
        if (!matches) throw BusinessException(ErrorCode.UNAUTHORIZED)
    }

    private fun JobExecution.toResponse(): DevBatchRunResponse =
        DevBatchRunResponse(
            jobName = jobInstance.jobName,
            jobInstanceId = jobInstance.instanceId,
            jobExecutionId = id,
            status = status,
        )
}
