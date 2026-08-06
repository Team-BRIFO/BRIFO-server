package com.brifo.server.batch.generation

import com.brifo.server.batch.collection.CollectionRound
import com.brifo.server.batch.collection.NewsCollectionJobConfiguration
import com.brifo.server.batch.common.BatchJobParameters
import com.brifo.server.batch.common.BusinessDateCalculator
import com.brifo.server.news.repository.NewsRepository
import org.slf4j.LoggerFactory
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.launch.JobOperator
import org.springframework.batch.core.repository.JobRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDate

@Component
@ConditionalOnProperty(prefix = "app.batch", name = ["scheduling-enabled"], havingValue = "true")
class NewsCardGenerationScheduler(
    private val jobOperator: JobOperator,
    private val jobRepository: JobRepository,
    @Qualifier(NewsCardGenerationJobConfiguration.JOB_NAME)
    private val job: Job,
    private val newsRepository: NewsRepository,
    private val businessDateCalculator: BusinessDateCalculator,
    private val clock: Clock,
) {
    @Scheduled(cron = "0 0 0 * * MON-FRI", zone = SEOUL_ZONE)
    fun generate() {
        val displayDate = LocalDate.now(clock)
        val targetDate = businessDateCalculator.previousBusinessDay(displayDate)
        if (!closingCollectionCompleted(targetDate)) {
            log.warn("마감 수집이 완료되지 않아 카드뉴스 생성을 건너뜁니다. targetDate={}", targetDate)
            return
        }
        if (newsRepository.findGenerationCandidateIds(targetDate.atStartOfDay(), targetDate.plusDays(1).atStartOfDay()).isEmpty()) {
            log.info("생성 대상 뉴스가 없어 카드뉴스 생성을 건너뜁니다. targetDate={}", targetDate)
            return
        }

        runCatching { jobOperator.start(job, BatchJobParameters.forDate(targetDate)) }
            .onFailure { log.error("카드뉴스 생성 배치 실행에 실패했습니다. targetDate={}", targetDate, it) }
    }

    private fun closingCollectionCompleted(targetDate: LocalDate): Boolean {
        val parameters = BatchJobParameters.forCollection(targetDate, CollectionRound.CLOSING.name)
        val instance = jobRepository.getJobInstance(NewsCollectionJobConfiguration.JOB_NAME, parameters) ?: return false
        return jobRepository.getLastJobExecution(instance)?.status == BatchStatus.COMPLETED
    }

    private companion object {
        const val SEOUL_ZONE = "Asia/Seoul"
        val log = LoggerFactory.getLogger(NewsCardGenerationScheduler::class.java)
    }
}
