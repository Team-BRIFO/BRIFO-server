package com.brifo.server.briefing.service.async

import com.brifo.server.briefing.client.BriefingAnalysisClient
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.UUID

/** 외부 분석 결과가 요청 대상과 의미적으로 일치하는지 검증한다. */
@Component
class BriefingAnalysisResultValidator {
    fun validate(
        context: BriefingAnalysisTask.Context,
        response: BriefingAnalysisClient.Result,
    ): BriefingAnalysisTask.ValidationResult {
        val requestedBriefingPublicIds = context.targets.map { it.briefingPublicId }
        if (hasInvalidBatch(response, requestedBriefingPublicIds.toSet())) {
            return BriefingAnalysisTask.ValidationResult(
                completions = emptyList(),
                failedBriefingPublicIds = requestedBriefingPublicIds,
            )
        }

        val results = response.briefings.associateBy { it.briefingId }
        val completions = mutableListOf<BriefingAnalysisTask.Completion>()
        val failedBriefingPublicIds = mutableListOf<UUID>()
        context.targets.forEach { target ->
            val result = results[target.briefingPublicId]
            if (result == null || !isValid(result, target)) {
                failedBriefingPublicIds += target.briefingPublicId
            } else {
                completions += result.toCompletion()
            }
        }

        return BriefingAnalysisTask.ValidationResult(
            completions = completions,
            failedBriefingPublicIds = failedBriefingPublicIds,
        )
    }

    private fun hasInvalidBatch(
        response: BriefingAnalysisClient.Result,
        requestedBriefingPublicIds: Set<UUID>,
    ): Boolean =
        response.briefings
            .groupingBy(BriefingAnalysisClient.BriefingResult::briefingId)
            .eachCount()
            .any { (briefingPublicId, count) ->
                count > 1 || briefingPublicId !in requestedBriefingPublicIds
            }

    private fun isValid(
        result: BriefingAnalysisClient.BriefingResult,
        target: BriefingAnalysisTask.Context.Target,
    ): Boolean =
        result.agentId == target.agentPublicId &&
            result.agentType == target.agentType &&
            result.modelName == target.modelName &&
            result.probability >= BigDecimal.ZERO &&
            result.probability <= BigDecimal.ONE &&
            result.headline.isNotBlank() &&
            result.summary.isNotBlank() &&
            result.commonAnalysis.isNotBlank() &&
            result.closingComment.isNotBlank()

    private fun BriefingAnalysisClient.BriefingResult.toCompletion(): BriefingAnalysisTask.Completion =
        BriefingAnalysisTask.Completion(
            briefingPublicId = briefingId,
            direction = direction,
            probability = probability,
            headline = headline,
            summary = summary,
            personalComment = personalComment,
            commonAnalysis = commonAnalysis,
            closingComment = closingComment,
        )
}
