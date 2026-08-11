package com.brifo.server.batch.settlement

import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.JobExecution
import org.springframework.batch.core.job.parameters.JobParameters
import org.springframework.batch.core.launch.JobOperator
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DecisionSettlementJobRunnerTest {
    @Test
    fun `수동 정산과 배치 정산은 동시에 실행되지 않는다`() {
        val jobOperator = mock(JobOperator::class.java)
        val job = mock(Job::class.java)
        val execution = mock(JobExecution::class.java)
        val firstStarted = CountDownLatch(1)
        val releaseFirst = CountDownLatch(1)
        val secondStarted = CountDownLatch(1)
        val calls = AtomicInteger()

        `when`(jobOperator.start(any(Job::class.java), any(JobParameters::class.java))).thenAnswer {
            when (calls.incrementAndGet()) {
                1 -> {
                    firstStarted.countDown()
                    assertTrue(releaseFirst.await(5, TimeUnit.SECONDS))
                }
                2 -> secondStarted.countDown()
            }
            execution
        }

        val runner = DecisionSettlementJobRunner(jobOperator, job)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val first = executor.submit<JobExecution> { runner.start(JobParameters()) }
            assertTrue(firstStarted.await(5, TimeUnit.SECONDS))

            val second = executor.submit<JobExecution> { runner.start(JobParameters()) }
            assertFalse(secondStarted.await(200, TimeUnit.MILLISECONDS))

            releaseFirst.countDown()
            first.get(5, TimeUnit.SECONDS)
            second.get(5, TimeUnit.SECONDS)
            assertTrue(secondStarted.await(5, TimeUnit.SECONDS))
            assertEquals(2, calls.get())
        } finally {
            releaseFirst.countDown()
            executor.shutdownNow()
        }
    }
}
