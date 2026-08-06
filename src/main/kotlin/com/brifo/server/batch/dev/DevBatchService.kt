package com.brifo.server.batch.dev

import com.brifo.server.batch.collection.NewsCollectionJobConfiguration
import com.brifo.server.batch.common.BatchJobParameters
import com.brifo.server.batch.generation.NewsCardGenerationJobConfiguration
import com.brifo.server.batch.settlement.DecisionSettlementJobConfiguration
import com.brifo.server.global.code.ErrorCode
import com.brifo.server.global.exception.BusinessException
import com.brifo.server.user.repository.UserRepository
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.JobExecution
import org.springframework.batch.core.launch.JobOperator
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
@Profile("dev")
@ConditionalOnProperty(prefix = "app.dev-auth", name = ["enabled"], havingValue = "true")
class DevBatchService(
    private val userRepository: UserRepository,
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
        userPublicId: UUID,
        request: DevNewsCollectionBatchRequest,
    ): DevBatchRunResponse {
        verifyDevUser(userPublicId)
        cleanupService.cleanupForCollection(request.targetDate, request.collectionRound)
        val execution = jobOperator.start(
            newsCollectionJob,
            BatchJobParameters.forDevCollection(request.targetDate, request.collectionRound.name),
        )
        return execution.toResponse()
    }

    @Synchronized
    fun rerunNewsCardGeneration(
        userPublicId: UUID,
        targetDate: LocalDate,
    ): DevBatchRunResponse {
        verifyDevUser(userPublicId)
        cleanupService.cleanupForGeneration(targetDate)
        val execution = jobOperator.start(newsCardGenerationJob, BatchJobParameters.forDevDate(targetDate))
        return execution.toResponse()
    }

    @Synchronized
    fun rerunDecisionSettlement(
        userPublicId: UUID,
        targetDate: LocalDate,
    ): DevBatchRunResponse {
        verifyDevUser(userPublicId)
        cleanupService.cleanupForSettlement(targetDate)
        val execution = jobOperator.start(decisionSettlementJob, BatchJobParameters.forDevDate(targetDate))
        return execution.toResponse()
    }

    private fun verifyDevUser(userPublicId: UUID) {
        val user = userRepository.findByPublicId(userPublicId) ?: throw BusinessException(ErrorCode.FORBIDDEN)
        if (!user.socialId.startsWith(DEV_SOCIAL_ID_PREFIX)) throw BusinessException(ErrorCode.FORBIDDEN)
    }

    private fun JobExecution.toResponse(): DevBatchRunResponse =
        DevBatchRunResponse(
            jobName = jobInstance.jobName,
            jobInstanceId = jobInstance.instanceId,
            jobExecutionId = id,
            status = status,
        )

    private companion object {
        const val DEV_SOCIAL_ID_PREFIX = "dev:"
    }
}
