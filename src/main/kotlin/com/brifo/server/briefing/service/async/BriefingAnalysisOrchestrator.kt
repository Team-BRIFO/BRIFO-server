package com.brifo.server.briefing.service.async

import com.brifo.server.briefing.client.BriefingAnalysisClient
import org.slf4j.LoggerFactory
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
            command.briefingPublicIds.forEach(::failAndRefundSafely)
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
        validationResult.failedBriefingPublicIds.forEach(::failAndRefundSafely)

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
                failAndRefundSafely(completion.briefingPublicId)
            }
        }
    }

    private fun failAndRefundSafely(briefingPublicId: UUID) {
        try {
            transactionService.failAndRefund(briefingPublicId)
        } catch (exception: Exception) {
            logger.error(
                "Briefing refund failed: {}",
                briefingPublicId,
                exception,
            )
        }
    }

    private fun BriefingAnalysisTask.Context.toClientRequest(): BriefingAnalysisClient.Request =
        BriefingAnalysisClient.Request(
            userId = userPublicId,
            newsCard = newsCards.map { card ->
                BriefingAnalysisClient.NewsCard(
                    cardId = card.cardPublicId,
                    newsId = card.newsPublicId,
                    headline = card.headline,
                    points = card.points,
                )
            },
            agentTypes = targets.map { it.agentType },
            levelRange = levelRange(targets.maxOf { it.level }),
            recentDecisions = recentDecisions.map { decision ->
                BriefingAnalysisClient.RecentDecision(
                    stockName = decision.stockName,
                    direction = decision.direction,
                    confidence = decision.confidence,
                    isCorrect = decision.isCorrect,
                    actualChange = decision.actualChange,
                )
            },
        )

    private fun levelRange(level: Int): String =
        when (level) {
            in 1..3 -> "1-3"
            in 4..6 -> "4-6"
            in 7..10 -> "7-10"
            else -> error("지원하지 않는 에이전트 레벨입니다: $level")
        }
}
