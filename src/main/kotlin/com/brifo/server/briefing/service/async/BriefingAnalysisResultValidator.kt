package com.brifo.server.briefing.service.async

import com.brifo.server.agent.entity.AgentType
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
        val requestedAgentTypes = context.targets.map { it.agentType }.toSet()
        if (hasInvalidBatch(response, requestedAgentTypes)) {
            return BriefingAnalysisTask.ValidationResult(
                completions = emptyList(),
                failedBriefingPublicIds = requestedBriefingPublicIds,
            )
        }

        val results = response.briefings.associateBy { it.agentType }
        val completions = mutableListOf<BriefingAnalysisTask.Completion>()
        val failedBriefingPublicIds = mutableListOf<UUID>()
        context.targets.forEach { target ->
            val result = results[target.agentType]
            if (result == null || !isValid(result, target)) {
                failedBriefingPublicIds += target.briefingPublicId
            } else {
                completions += result.toCompletion(target.briefingPublicId)
            }
        }

        return BriefingAnalysisTask.ValidationResult(
            completions = completions,
            failedBriefingPublicIds = failedBriefingPublicIds,
        )
    }

    private fun hasInvalidBatch(
        response: BriefingAnalysisClient.Result,
        requestedAgentTypes: Set<AgentType>,
    ): Boolean =
        response.briefings
            .groupingBy(BriefingAnalysisClient.BriefingResult::agentType)
            .eachCount()
            .any { (agentType, count) ->
                count > 1 || agentType !in requestedAgentTypes
            }

    private fun isValid(
        result: BriefingAnalysisClient.BriefingResult,
        target: BriefingAnalysisTask.Context.Target,
    ): Boolean =
        result.agentType == target.agentType &&
            result.modelName == target.modelName &&
            result.confidenceRate in 0..100 &&
            result.headline.isNotBlank() &&
            result.summary.isNotBlank() &&
            result.contentText.isNotBlank() &&
            result.oneLiner.isNotBlank()

    private fun BriefingAnalysisClient.BriefingResult.toCompletion(briefingPublicId: UUID): BriefingAnalysisTask.Completion =
        BriefingAnalysisTask.Completion(
            briefingPublicId = briefingPublicId,
            direction = direction,
            probability = BigDecimal(confidenceRate).movePointLeft(2),
            headline = headline,
            summary = summary,
            personalComment = personalComment,
            commonAnalysis = contentText,
            closingComment = oneLiner,
        )
}
