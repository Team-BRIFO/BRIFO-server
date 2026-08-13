package com.brifo.server.batch.price

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
class DailyClosingPriceScheduler(
    private val jobOperator: JobOperator,
    @Qualifier(DailyClosingPriceJobConfiguration.JOB_NAME)
    private val job: Job,
    private val clock: Clock,
) {
    @Scheduled(cron = "0 0 16 * * MON-FRI", zone = SEOUL_ZONE)
    fun backfill() {
        val targetDate = LocalDate.now(clock)
        runCatching {
            jobOperator.start(job, BatchJobParameters.forDate(targetDate))
        }.onFailure { exception ->
            log.error("일별 종가 백필 배치 실행에 실패했습니다. targetDate={}", targetDate, exception)
        }
    }

    private companion object {
        const val SEOUL_ZONE = "Asia/Seoul"
        val log = LoggerFactory.getLogger(DailyClosingPriceScheduler::class.java)
    }
}
