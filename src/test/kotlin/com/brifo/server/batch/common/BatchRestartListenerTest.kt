package com.brifo.server.batch.common

import org.junit.jupiter.api.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.configuration.JobRegistry
import org.springframework.batch.core.job.JobExecution
import org.springframework.batch.core.job.JobInstance
import org.springframework.batch.core.launch.JobOperator
import org.springframework.batch.core.repository.JobRepository
import org.springframework.scheduling.TaskScheduler
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class BatchRestartListenerTest {
    private val jobRegistry = mock(JobRegistry::class.java)
    private val jobOperator = mock(JobOperator::class.java)
    private val jobRepository = mock(JobRepository::class.java)
    private val taskScheduler = mock(TaskScheduler::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-08-03T06:50:00Z"), ZoneOffset.UTC)
    private val listener =
        BatchRestartListener(
            jobRegistry,
            jobOperator,
            jobRepository,
            taskScheduler,
            BatchProperties(restartDelay = Duration.ofMinutes(10), maxExecutions = 3),
            clock,
        )

    @Test
    fun `최초 실패이면 10분 후 재시작을 예약한다`() {
        val execution = failedExecution()
        `when`(jobRepository.getJobExecutions(execution.jobInstance)).thenReturn(listOf(execution))

        listener.afterJob(execution)

        verify(taskScheduler).schedule(any(Runnable::class.java), any(Instant::class.java))
    }

    @Test
    fun `실행을 세 번 소진하면 더 이상 재시작하지 않는다`() {
        val execution = failedExecution()
        `when`(jobRepository.getJobExecutions(execution.jobInstance)).thenReturn(
            listOf(execution, mock(JobExecution::class.java), mock(JobExecution::class.java)),
        )

        listener.afterJob(execution)

        verify(taskScheduler, never()).schedule(any(Runnable::class.java), any(Instant::class.java))
    }

    private fun failedExecution(): JobExecution {
        val execution = mock(JobExecution::class.java)
        val instance = mock(JobInstance::class.java)
        `when`(execution.status).thenReturn(BatchStatus.FAILED)
        `when`(execution.jobInstance).thenReturn(instance)
        `when`(execution.jobParameters).thenReturn(BatchJobParameters.forDate(LocalDate.of(2026, 8, 3)))
        `when`(execution.stepExecutions).thenReturn(emptySet())
        `when`(instance.jobName).thenReturn("testJob")
        `when`(instance.instanceId).thenReturn(1L)
        return execution
    }
}
