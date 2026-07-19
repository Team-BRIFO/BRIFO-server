package com.brifo.server.briefing.service.sync

import com.brifo.server.agent.entity.AgentType
import com.brifo.server.briefing.service.async.BriefingAnalysisOrchestrator
import com.brifo.server.briefing.service.async.BriefingAnalysisTask
import com.brifo.server.briefing.service.sync.BriefingRequestOrchestrator
import com.brifo.server.briefing.service.sync.BriefingRequestTask
import com.brifo.server.briefing.service.sync.BriefingRequestTransactionService
import com.brifo.server.briefing.service.sync.BriefingRequestValidator
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BriefingRequestOrchestratorTest {
    private val validator = mock(BriefingRequestValidator::class.java)
    private val transactionService = mock(BriefingRequestTransactionService::class.java)
    private val analysisOrchestrator = mock(BriefingAnalysisOrchestrator::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-07-18T06:19:59Z"), ZoneId.of("Asia/Seoul"))
    private val orchestrator = BriefingRequestOrchestrator(
        validator = validator,
        transactionService = transactionService,
        analysisOrchestrator = analysisOrchestrator,
        clock = clock,
    )

    @Test
    fun `요청 검증과 트랜잭션 성공 후 분석을 제출하고 접수 결과를 반환한다`() {
        val userId = UUID.randomUUID()
        val stockId = UUID.randomUUID()
        val agentId = UUID.randomUUID()
        val briefingId = UUID.randomUUID()
        val command = command(userId, stockId, listOf(agentId))
        val result = BriefingRequestTask.Result(
            requestedCount = 1,
            totalSalaryCost = 10,
            requestedAgents = listOf(
                BriefingRequestTask.Result.RequestedAgent(briefingId, agentId, AgentType.ROOKIE, 10),
            ),
        )
        `when`(transactionService.request(command)).thenReturn(result)

        val response = orchestrator.request(userId, stockId, listOf(agentId))

        verify(validator).validate(command)
        verify(transactionService).request(command)
        verify(analysisOrchestrator).processAsync(
            BriefingAnalysisTask.Command(userId, listOf(briefingId)),
        )
        assertEquals(1, response.requestedCount)
        assertEquals(10, response.totalSalaryCost)
        assertEquals(briefingId, response.requestedAgents.single().briefingId)
        assertEquals(15, command.requestedAt.hour)
        assertEquals(19, command.requestedAt.minute)
    }

    @Test
    fun `사전 검증이 실패하면 트랜잭션과 비동기 분석을 시작하지 않는다`() {
        val userId = UUID.randomUUID()
        val stockId = UUID.randomUUID()
        val agentIds = listOf(UUID.randomUUID())
        val command = command(userId, stockId, agentIds)
        doThrow(IllegalArgumentException("invalid")).`when`(validator).validate(command)

        assertFailsWith<IllegalArgumentException> {
            orchestrator.request(userId, stockId, agentIds)
        }

        verifyNoInteractions(transactionService, analysisOrchestrator)
    }

    @Test
    fun `요청 트랜잭션이 실패하면 비동기 분석을 제출하지 않는다`() {
        val userId = UUID.randomUUID()
        val stockId = UUID.randomUUID()
        val agentIds = listOf(UUID.randomUUID())
        val command = command(userId, stockId, agentIds)
        `when`(transactionService.request(command))
            .thenThrow(IllegalStateException("transaction failed"))

        assertFailsWith<IllegalStateException> {
            orchestrator.request(userId, stockId, agentIds)
        }

        verify(validator).validate(command)
        verifyNoMoreInteractions(analysisOrchestrator)
    }

    private fun command(
        userId: UUID,
        stockId: UUID,
        agentIds: List<UUID>,
    ) = BriefingRequestTask.Command(
        userPublicId = userId,
        stockPublicId = stockId,
        agentPublicIds = agentIds,
        requestedAt = java.time.LocalDateTime.of(2026, 7, 18, 15, 19, 59),
    )
}
