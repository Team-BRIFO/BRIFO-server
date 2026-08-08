package com.brifo.server.batch.settlement

import com.brifo.server.batch.common.BatchJobParameters
import com.brifo.server.decision.repository.DecisionRepository
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
class DecisionSettlementScheduler(
    private val jobOperator: JobOperator,
    @Qualifier(DecisionSettlementJobConfiguration.JOB_NAME)
    private val job: Job,
    private val decisionRepository: DecisionRepository,
    private val clock: Clock,
) {
    @Scheduled(cron = "0 50 15 * * MON-FRI", zone = SEOUL_ZONE)
    fun settle() {
        val targetDate = LocalDate.now(clock)
        if (decisionRepository.findUnsettledIds(targetDate).isEmpty()) {
            log.info("정산 대상 결정이 없어 정산을 건너뜁니다. targetDate={}", targetDate)
            return
        }
        runCatching { jobOperator.start(job, BatchJobParameters.forDate(targetDate)) }
            .onFailure { log.error("결정 정산 배치 실행에 실패했습니다. targetDate={}", targetDate, it) }
    }

    private companion object {
        const val SEOUL_ZONE = "Asia/Seoul"
        val log = LoggerFactory.getLogger(DecisionSettlementScheduler::class.java)
    }
}
