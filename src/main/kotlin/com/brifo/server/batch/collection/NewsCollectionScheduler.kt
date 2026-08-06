package com.brifo.server.batch.collection

import com.brifo.server.batch.common.BatchJobParameters
import org.slf4j.LoggerFactory
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.launch.JobOperator
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDate

@Component
@ConditionalOnProperty(prefix = "app.batch", name = ["scheduling-enabled"], havingValue = "true")
class NewsCollectionScheduler(
    private val jobOperator: JobOperator,
    @Qualifier(NewsCollectionJobConfiguration.JOB_NAME)
    private val job: Job,
    private val clock: Clock,
) {
    @Scheduled(cron = "0 0 7 * * MON-FRI", zone = SEOUL_ZONE)
    fun collectMorning() = launch(CollectionRound.MORNING)

    @Scheduled(cron = "0 30 11 * * MON-FRI", zone = SEOUL_ZONE)
    fun collectMidday() = launch(CollectionRound.MIDDAY)

    @Scheduled(cron = "0 40 15 * * MON-FRI", zone = SEOUL_ZONE)
    fun collectClosing() = launch(CollectionRound.CLOSING)

    private fun launch(round: CollectionRound) {
        val targetDate = LocalDate.now(clock)
        runCatching {
            jobOperator.start(job, BatchJobParameters.forCollection(targetDate, round.name))
        }.onFailure { exception ->
            log.error("뉴스 수집 배치 실행에 실패했습니다. targetDate={}, round={}", targetDate, round, exception)
        }
    }

    private companion object {
        const val SEOUL_ZONE = "Asia/Seoul"
        val log = LoggerFactory.getLogger(NewsCollectionScheduler::class.java)
    }
}
