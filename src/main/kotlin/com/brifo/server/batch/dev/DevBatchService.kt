package com.brifo.server.batch.dev

import com.brifo.server.auth.dev.DevAuthProperties
import com.brifo.server.batch.collection.NewsCollectionJobConfiguration
import com.brifo.server.batch.common.BatchJobParameters
import com.brifo.server.batch.generation.NewsCardGenerationItemProcessor
import com.brifo.server.batch.generation.NewsCardGenerationJobConfiguration
import com.brifo.server.batch.generation.NewsCardPersistenceService
import com.brifo.server.batch.settlement.DecisionSettlementJobConfiguration
import com.brifo.server.batch.settlement.DecisionSettlementJobRunner
import com.brifo.server.global.code.ErrorCode
import com.brifo.server.global.exception.BusinessException
import com.brifo.server.news.client.NewsCardGenerationClient
import com.brifo.server.news.repository.NewsCardRepository
import com.brifo.server.news.repository.NewsRepository
import com.brifo.server.term.repository.NewsCardTermRepository
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.JobExecution
import org.springframework.batch.core.launch.JobOperator
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Clock
import java.time.LocalDate

@Service
@Profile("dev")
@ConditionalOnProperty(prefix = "app.dev-auth", name = ["enabled"], havingValue = "true")
class DevBatchService(
    private val properties: DevAuthProperties,
    private val cleanupService: DevBatchCleanupService,
    private val clock: Clock,
    private val newsRepository: NewsRepository,
    private val newsCardRepository: NewsCardRepository,
    private val newsCardTermRepository: NewsCardTermRepository,
    private val newsCardGenerationClient: NewsCardGenerationClient,
    private val newsCardPersistenceService: NewsCardPersistenceService,
    private val jobOperator: JobOperator,
    @Qualifier(NewsCollectionJobConfiguration.JOB_NAME)
    private val newsCollectionJob: Job,
    @Qualifier(NewsCardGenerationJobConfiguration.JOB_NAME)
    private val newsCardGenerationJob: Job,
    @Qualifier(DecisionSettlementJobConfiguration.JOB_NAME)
    private val decisionSettlementJob: Job,
    private val decisionSettlementJobRunner: DecisionSettlementJobRunner? = null,
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
    @Transactional
    fun runSingleNewsCardGeneration(
        request: DevSingleNewsCardGenerationRequest,
    ): DevSingleNewsCardGenerationResponse {
        verifyPassword(request.password)
        val news = newsRepository.findByPublicId(request.newsId)
            ?: throw BusinessException(ErrorCode.NOT_FOUND, "뉴스를 찾을 수 없습니다: ${request.newsId}")
        val newsId = requireNotNull(news.id) { "저장된 뉴스에는 id가 있어야 합니다." }

        cleanupService.cleanupForSingleGeneration(newsId)

        val processor = NewsCardGenerationItemProcessor(
            newsRepository = newsRepository,
            newsCardTermRepository = newsCardTermRepository,
            client = newsCardGenerationClient,
        )
        val generated = processor.process(newsId)
        val displayDate = LocalDate.now(clock)
        newsCardPersistenceService.save(generated, displayDate)
        val newsCard = newsCardRepository.findByNewsId(newsId)
            ?: throw IllegalStateException("생성된 카드뉴스를 찾을 수 없습니다: $newsId")

        return DevSingleNewsCardGenerationResponse(
            newsId = request.newsId,
            newsCardId = requireNotNull(newsCard.publicId) { "저장된 카드뉴스에는 publicId가 있어야 합니다." },
            displayDate = displayDate,
        )
    }

    @Synchronized
    fun rerunDecisionSettlement(
        request: DevDateBatchRequest,
    ): DevBatchRunResponse {
        verifyPassword(request.password)
        val parameters = BatchJobParameters.forDevDate(request.targetDate)
        val execution = decisionSettlementJobRunner?.start(parameters) {
            cleanupService.cleanupForSettlement(request.targetDate)
        } ?: run {
            cleanupService.cleanupForSettlement(request.targetDate)
            jobOperator.start(decisionSettlementJob, parameters)
        }
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
