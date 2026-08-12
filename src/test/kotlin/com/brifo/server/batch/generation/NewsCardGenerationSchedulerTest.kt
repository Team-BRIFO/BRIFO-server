package com.brifo.server.batch.generation

import com.brifo.server.news.repository.NewsRepository
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.JobExecution
import org.springframework.batch.core.job.JobInstance
import org.springframework.batch.core.job.parameters.JobParameters
import org.springframework.batch.core.launch.JobOperator
import org.springframework.batch.core.repository.JobRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class NewsCardGenerationSchedulerTest {
    @Test
    fun `최초 생성 실행 실패는 지연 목록에서 재시도한다`() {
        val jobOperator = mock(JobOperator::class.java)
        val jobRepository = mock(JobRepository::class.java)
        val job = mock(Job::class.java)
        val newsRepository = mock(NewsRepository::class.java)
        val instance = mock(JobInstance::class.java)
        val collectionExecution = mock(JobExecution::class.java)
        val generationExecution = mock(JobExecution::class.java)
        val clock = Clock.fixed(Instant.parse("2026-07-20T00:00:00Z"), ZoneId.of("Asia/Seoul"))
        val targetDate = LocalDate.of(2026, 7, 19)
        val scheduler = NewsCardGenerationScheduler(
            jobOperator,
            jobRepository,
            job,
            newsRepository,
            clock,
        )
        `when`(jobRepository.getJobInstance(any(String::class.java), any(JobParameters::class.java))).thenReturn(instance)
        `when`(jobRepository.getLastJobExecution(instance)).thenReturn(collectionExecution)
        `when`(collectionExecution.status).thenReturn(BatchStatus.COMPLETED)
        `when`(
            newsRepository.findGenerationCandidateIds(targetDate.atStartOfDay(), targetDate.plusDays(1).atStartOfDay()),
        ).thenReturn(listOf(1L))
        `when`(jobOperator.start(any(Job::class.java), any(JobParameters::class.java)))
            .thenThrow(IllegalStateException("temporary failure"))
            .thenReturn(generationExecution)

        scheduler.generate()
        scheduler.retryDeferred()

        verify(jobOperator, times(2)).start(any(Job::class.java), any(JobParameters::class.java))
    }
}
