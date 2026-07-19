package com.brifo.server.briefing.service.async

import com.brifo.server.briefing.client.BriefingAnalysisClient
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

/** 접수된 브리핑의 외부 분석 호출과 성공·실패 처리를 비동기로 조율한다. */
@Service
class BriefingAnalysisOrchestrator(
    private val transactionService: BriefingAnalysisTransactionService,
    private val analysisClient: BriefingAnalysisClient,
    private val resultValidator: BriefingAnalysisResultValidator,
) {
    private val logger = LoggerFactory.getLogger(BriefingAnalysisOrchestrator::class.java)

    @Async
    fun processAsync(command: BriefingAnalysisTask.Command) {
        val context = transactionService.start(command) ?: return
        val response = try {
            analysisClient.createBriefings(context.toClientRequest())
        } catch (exception: Exception) {
            // 외부 분석 호출이 최종 실패하면 요청 묶음 전체를 실패 처리하고 환불한다.
            logger.error(
                "Briefing analysis failed; refunding briefings: {}",
                command.briefingPublicIds,
                exception,
            )
            command.briefingPublicIds.forEach(transactionService::failAndRefund)
            return
        }

        val validationResult = resultValidator.validate(context, response)

        // 외부 응답 검증에 실패한 브리핑만 개별 실패 처리하고 환불한다.
        if (validationResult.failedBriefingPublicIds.isNotEmpty()) {
            logger.warn(
                "Briefing result validation failed; refunding briefings: {}",
                validationResult.failedBriefingPublicIds,
            )
        }
        validationResult.failedBriefingPublicIds.forEach(transactionService::failAndRefund)

        // 검증을 통과한 결과는 개별 완료하고, 저장 실패 시 해당 브리핑만 실패 처리하고 환불한다.
        validationResult.completions.forEach { completion ->
            try {
                transactionService.complete(completion)
            } catch (exception: Exception) {
                logger.error(
                    "Briefing completion failed; refunding briefing: {}",
                    completion.briefingPublicId,
                    exception,
                )
                transactionService.failAndRefund(completion.briefingPublicId)
            }
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
