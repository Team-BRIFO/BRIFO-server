package com.brifo.server.batch.settlement

import com.brifo.server.batch.common.BatchJobParameters
import com.brifo.server.decision.repository.DecisionRepository
import com.brifo.server.stock.repository.PendingUserStockRepository
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.parameters.JobParameters
import org.springframework.batch.core.launch.JobOperator
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.assertEquals

class DecisionSettlementSchedulerTest {
    private val jobOperator = mock(JobOperator::class.java)
    private val job = mock(Job::class.java)
    private val decisionRepository = mock(DecisionRepository::class.java)
    private val pendingUserStockRepository = mock(PendingUserStockRepository::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-08-03T06:50:00Z"), ZoneId.of("Asia/Seoul"))
    private val scheduler =
        DecisionSettlementScheduler(jobOperator, job, decisionRepository, pendingUserStockRepository, clock)

    @Test
    fun `미정산 결정이 없으면 정산 Job을 실행하지 않는다`() {
        `when`(decisionRepository.findUnsettledIds(java.time.LocalDate.of(2026, 8, 3))).thenReturn(emptyList())

        scheduler.settle()

        assertEquals(0, org.mockito.Mockito.mockingDetails(jobOperator).invocations.count { it.method.name == "start" })
    }

    @Test
    fun `미정산 결정이 있으면 당일을 식별값으로 정산 Job을 실행한다`() {
        `when`(decisionRepository.findUnsettledIds(java.time.LocalDate.of(2026, 8, 3))).thenReturn(listOf(1L))

        scheduler.settle()

        val parameters = org.mockito.Mockito.mockingDetails(jobOperator).invocations
            .single { it.method.name == "start" }.arguments[1] as JobParameters
        assertEquals("2026-08-03", parameters.getString(BatchJobParameters.TARGET_DATE))
    }

    @Test
    fun `미정산 결정이 없어도 적용할 관심 종목이 있으면 정산 Job을 실행한다`() {
        `when`(decisionRepository.findUnsettledIds(java.time.LocalDate.of(2026, 8, 3))).thenReturn(emptyList())
        `when`(
            pendingUserStockRepository.existsByEffectiveAtLessThanEqual(
                LocalDateTime.of(2026, 8, 3, 15, 50),
            ),
        ).thenReturn(true)

        scheduler.settle()

        val parameters = org.mockito.Mockito.mockingDetails(jobOperator).invocations
            .single { it.method.name == "start" }.arguments[1] as JobParameters
        assertEquals("2026-08-03", parameters.getString(BatchJobParameters.TARGET_DATE))
    }
}
