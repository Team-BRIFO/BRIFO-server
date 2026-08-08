package com.brifo.server.batch.common

import org.slf4j.LoggerFactory
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.configuration.JobRegistry
import org.springframework.batch.core.job.JobExecution
import org.springframework.batch.core.launch.JobOperator
import org.springframework.batch.core.listener.JobExecutionListener
import org.springframework.batch.core.repository.JobRepository
import org.springframework.scheduling.TaskScheduler
import org.springframework.stereotype.Component
import java.time.Clock

@Component
class BatchRestartListener(
    private val jobRegistry: JobRegistry,
    private val jobOperator: JobOperator,
    private val jobRepository: JobRepository,
    private val taskScheduler: TaskScheduler,
    private val properties: BatchProperties,
    private val clock: Clock,
) : JobExecutionListener {
    override fun afterJob(jobExecution: JobExecution) {
        if (jobExecution.status != BatchStatus.FAILED) return

        val executionCount = jobRepository.getJobExecutions(jobExecution.jobInstance).size
        if (executionCount >= properties.maxExecutions) {
            log.error(
                "event=BATCH_FINAL_FAILED jobName={} targetDate={} jobInstanceId={} jobExecutionId={} " +
                    "failedSteps={} executionCount={} exception={}",
                jobExecution.jobInstance.jobName,
                jobExecution.jobParameters.getString(BatchJobParameters.TARGET_DATE),
                jobExecution.jobInstance.instanceId,
                jobExecution.id,
                jobExecution.stepExecutions.filter { it.status == BatchStatus.FAILED }.joinToString { it.stepName },
                executionCount,
                jobExecution.allFailureExceptions.joinToString(" | ") {
                    "${it::class.simpleName}: ${it.message}"
                },
            )
            return
        }

        val restartAt = clock.instant().plus(properties.restartDelay)
        taskScheduler.schedule(
            {
                runCatching {
                    jobOperator.start(
                        checkNotNull(jobRegistry.getJob(jobExecution.jobInstance.jobName)) {
                            "배치 Job이 등록되지 않았습니다: ${jobExecution.jobInstance.jobName}"
                        },
                        jobExecution.jobParameters,
                    )
                }.onFailure { exception ->
                    log.error(
                        "배치 Job 재시작에 실패했습니다. jobName={}, jobInstanceId={}",
                        jobExecution.jobInstance.jobName,
                        jobExecution.jobInstance.instanceId,
                        exception,
                    )
                }
            },
            restartAt,
        )
        log.warn(
            "배치 재시작을 예약했습니다. jobName={}, jobInstanceId={}, executionCount={}, restartAt={}",
            jobExecution.jobInstance.jobName,
            jobExecution.jobInstance.instanceId,
            executionCount,
            restartAt,
        )
    }

    private companion object {
        val log = LoggerFactory.getLogger(BatchRestartListener::class.java)
    }
}
