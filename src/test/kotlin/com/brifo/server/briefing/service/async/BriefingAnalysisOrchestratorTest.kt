package com.brifo.server.briefing.service.async

import com.brifo.server.agent.entity.AgentType
import com.brifo.server.briefing.client.BriefingAnalysisClient
import com.brifo.server.briefing.entity.BriefingDirection
import com.brifo.server.briefing.service.async.BriefingAnalysisOrchestrator
import com.brifo.server.briefing.service.async.BriefingAnalysisResultValidator
import com.brifo.server.briefing.service.async.BriefingAnalysisTask
import com.brifo.server.briefing.service.async.BriefingAnalysisTransactionService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.util.UUID

class BriefingAnalysisOrchestratorTest {
    private val transactionService = mock(BriefingAnalysisTransactionService::class.java)
    private val analysisClient = mock(BriefingAnalysisClient::class.java)
    private val resultValidator = mock(BriefingAnalysisResultValidator::class.java)
    private val orchestrator = BriefingAnalysisOrchestrator(transactionService, analysisClient, resultValidator)

    @Test
    fun `분석 시작 대상이 아니면 외부 요청과 후속 처리를 하지 않는다`() {
        val command = command()
        `when`(transactionService.start(command)).thenReturn(null)

        orchestrator.processAsync(command)

        verifyNoInteractions(analysisClient, resultValidator)
        verify(transactionService).start(command)
        verifyNoMoreInteractions(transactionService)
    }

    @Test
    fun `외부 분석 요청이 실패하면 요청한 브리핑을 모두 실패 환불한다`() {
        val command = command(UUID.randomUUID(), UUID.randomUUID())
        val context = context(command)
        `when`(transactionService.start(command)).thenReturn(context)
        `when`(analysisClient.createBriefings(clientRequest(context)))
            .thenThrow(IllegalStateException("external failure"))

        orchestrator.processAsync(command)

        command.briefingPublicIds.forEach { verify(transactionService).failAndRefund(it) }
        verifyNoInteractions(resultValidator)
    }

    @Test
    fun `검증 결과의 성공과 실패 브리핑을 각각 완료하고 환불한다`() {
        val successId = UUID.randomUUID()
        val failureId = UUID.randomUUID()
        val command = command(successId, failureId)
        val context = context(command)
        val response = BriefingAnalysisClient.Result(emptyList())
        val completion = completion(successId)
        `when`(transactionService.start(command)).thenReturn(context)
        `when`(analysisClient.createBriefings(clientRequest(context))).thenReturn(response)
        `when`(resultValidator.validate(context, response)).thenReturn(
            BriefingAnalysisTask.ValidationResult(listOf(completion), listOf(failureId)),
        )

        orchestrator.processAsync(command)

        verify(transactionService).complete(completion)
        verify(transactionService).failAndRefund(failureId)
    }

    @Test
    fun `완료 저장 중 실패하면 해당 브리핑을 실패 환불한다`() {
        val briefingId = UUID.randomUUID()
        val command = command(briefingId)
        val context = context(command)
        val response = BriefingAnalysisClient.Result(emptyList())
        val completion = completion(briefingId)
        `when`(transactionService.start(command)).thenReturn(context)
        `when`(analysisClient.createBriefings(clientRequest(context))).thenReturn(response)
        `when`(resultValidator.validate(context, response)).thenReturn(
            BriefingAnalysisTask.ValidationResult(listOf(completion), emptyList()),
        )
        doThrow(IllegalStateException("persistence failure")).`when`(transactionService).complete(completion)

        orchestrator.processAsync(command)

        verify(transactionService).failAndRefund(briefingId)
    }

    @Test
    fun `유효한 외부 결과는 실패 환불 없이 완료한다`() {
        val briefingId = UUID.randomUUID()
        val command = command(briefingId)
        val context = context(command)
        val response = BriefingAnalysisClient.Result(emptyList())
        val completion = completion(briefingId)
        `when`(transactionService.start(command)).thenReturn(context)
        `when`(analysisClient.createBriefings(clientRequest(context))).thenReturn(response)
        `when`(resultValidator.validate(context, response)).thenReturn(
            BriefingAnalysisTask.ValidationResult(listOf(completion), emptyList()),
        )

        orchestrator.processAsync(command)

        verify(transactionService).complete(completion)
        verify(transactionService).start(command)
        verifyNoMoreInteractions(transactionService)
    }

    private fun command(vararg briefingIds: UUID): BriefingAnalysisTask.Command =
        BriefingAnalysisTask.Command(UUID.randomUUID(), briefingIds.toList().ifEmpty { listOf(UUID.randomUUID()) })

    private fun context(command: BriefingAnalysisTask.Command): BriefingAnalysisTask.Context =
        BriefingAnalysisTask.Context(
            userPublicId = command.userPublicId,
            newsCardPublicIds = listOf(UUID.randomUUID(), UUID.randomUUID()),
            targets = command.briefingPublicIds.map {
                BriefingAnalysisTask.Context.Target(it, UUID.randomUUID(), AgentType.ROOKIE, "model")
            },
            recentDecisionPublicIds = emptyList(),
        )

    private fun completion(briefingId: UUID): BriefingAnalysisTask.Completion =
        BriefingAnalysisTask.Completion(
            briefingPublicId = briefingId,
            direction = BriefingDirection.UP,
            probability = BigDecimal("0.72"),
            headline = "헤드라인",
            summary = "요약",
            personalComment = null,
            commonAnalysis = "분석",
            closingComment = "의견",
        )

    private fun clientRequest(context: BriefingAnalysisTask.Context): BriefingAnalysisClient.Request =
        BriefingAnalysisClient.Request(
            userId = context.userPublicId,
            newsCardIds = context.newsCardPublicIds,
            targets = context.targets.map {
                BriefingAnalysisClient.Target(it.briefingPublicId, it.agentPublicId)
            },
            recentDecisionIds = context.recentDecisionPublicIds,
        )
}
