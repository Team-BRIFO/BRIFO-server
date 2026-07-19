package com.brifo.server.briefing.service.async

import com.brifo.server.briefing.client.BriefingAnalysisClient
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.UUID

/** 접수된 브리핑의 외부 분석 호출과 성공·실패 처리를 비동기로 조율한다. */
@Service
class BriefingAnalysisOrchestrator(
    private val transactionService: BriefingAnalysisTransactionService,
    private val analysisClient: BriefingAnalysisClient,
    private val resultValidator: BriefingAnalysisResultValidator,
) {
    @Async
    fun processAsync(command: BriefingAnalysisTask.Command) {
        val context = transactionService.start(command) ?: return
        val response = try {
            analysisClient.createBriefings(context.toClientRequest())
        } catch (exception: Exception) {
            failAndRefundAll(command.briefingPublicIds)
            return
        }

        val validationResult = resultValidator.validate(context, response)

        failAndRefundAll(validationResult.failedBriefingPublicIds)
        validationResult.completions.forEach(::completeOrFailAndRefund)
    }

    private fun failAndRefundAll(briefingPublicIds: List<UUID>) {
        briefingPublicIds.forEach(transactionService::failAndRefund)
    }

    private fun completeOrFailAndRefund(completion: BriefingAnalysisTask.Completion) {
        try {
            transactionService.complete(completion)
        } catch (exception: Exception) {
            transactionService.failAndRefund(completion.briefingPublicId)
        }
    }

    private fun BriefingAnalysisTask.Context.toClientRequest(): BriefingAnalysisClient.Request =
        BriefingAnalysisClient.Request(
            userId = userPublicId,
            newsCardIds = newsCardPublicIds,
            targets = targets.map { target ->
                BriefingAnalysisClient.Target(
                    briefingId = target.briefingPublicId,
                    agentId = target.agentPublicId,
                )
            },
            recentDecisionIds = recentDecisionPublicIds,
        )
}
