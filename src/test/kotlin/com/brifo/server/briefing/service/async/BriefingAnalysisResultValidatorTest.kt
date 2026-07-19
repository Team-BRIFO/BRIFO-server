package com.brifo.server.briefing.service.async

import com.brifo.server.agent.entity.AgentType
import com.brifo.server.briefing.client.BriefingAnalysisClient
import com.brifo.server.briefing.entity.BriefingDirection
import com.brifo.server.briefing.service.async.BriefingAnalysisResultValidator
import com.brifo.server.briefing.service.async.BriefingAnalysisTask
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BriefingAnalysisResultValidatorTest {
    private val validator = BriefingAnalysisResultValidator()

    @Test
    fun `요청 정보와 유효한 분석 값이 일치하면 완료 결과로 분류한다`() {
        val target = target()

        val result = validator.validate(context(target), response(briefingResult(target)))

        assertEquals(listOf(target.briefingPublicId), result.completions.map { it.briefingPublicId })
        assertTrue(result.failedBriefingPublicIds.isEmpty())
    }

    @Test
    fun `결과가 누락되면 해당 브리핑만 실패로 분류한다`() {
        val first = target()
        val second = target()

        val result = validator.validate(context(first, second), response(briefingResult(first)))

        assertEquals(listOf(first.briefingPublicId), result.completions.map { it.briefingPublicId })
        assertEquals(listOf(second.briefingPublicId), result.failedBriefingPublicIds)
    }

    @Test
    fun `사원 타입이나 모델이 다르면 해당 브리핑만 실패로 분류한다`() {
        val target = target()
        val mismatched = briefingResult(target).copy(agentType = AgentType.PRO, modelName = "other-model")

        val result = validator.validate(context(target), response(mismatched))

        assertTrue(result.completions.isEmpty())
        assertEquals(listOf(target.briefingPublicId), result.failedBriefingPublicIds)
    }

    @Test
    fun `확률이 범위를 벗어나거나 필수 문자열이 비어 있으면 해당 브리핑만 실패로 분류한다`() {
        val invalidProbabilityTarget = target()
        val blankContentTarget = target()

        val result = validator.validate(
            context(invalidProbabilityTarget, blankContentTarget),
            response(
                briefingResult(invalidProbabilityTarget).copy(probability = BigDecimal("1.01")),
                briefingResult(blankContentTarget).copy(summary = " "),
            ),
        )

        assertTrue(result.completions.isEmpty())
        assertEquals(
            listOf(invalidProbabilityTarget.briefingPublicId, blankContentTarget.briefingPublicId),
            result.failedBriefingPublicIds,
        )
    }

    @Test
    fun `결과에 중복되거나 요청하지 않은 브리핑이 있으면 전체 실패로 분류한다`() {
        val first = target()
        val second = target()
        val unknown = target()

        val duplicateResult = validator.validate(
            context(first, second),
            response(briefingResult(first), briefingResult(first)),
        )
        val unknownResult = validator.validate(
            context(first, second),
            response(briefingResult(first), briefingResult(unknown)),
        )

        val expectedFailures = listOf(first.briefingPublicId, second.briefingPublicId)
        assertEquals(expectedFailures, duplicateResult.failedBriefingPublicIds)
        assertEquals(expectedFailures, unknownResult.failedBriefingPublicIds)
        assertTrue(duplicateResult.completions.isEmpty())
        assertTrue(unknownResult.completions.isEmpty())
    }

    private fun target(): BriefingAnalysisTask.Context.Target =
        BriefingAnalysisTask.Context.Target(
            briefingPublicId = UUID.randomUUID(),
            agentPublicId = UUID.randomUUID(),
            agentType = AgentType.ROOKIE,
            modelName = "model",
        )

    private fun context(vararg targets: BriefingAnalysisTask.Context.Target): BriefingAnalysisTask.Context =
        BriefingAnalysisTask.Context(
            userPublicId = UUID.randomUUID(),
            newsCardPublicIds = listOf(UUID.randomUUID(), UUID.randomUUID()),
            targets = targets.toList(),
            recentDecisionPublicIds = emptyList(),
        )

    private fun response(vararg results: BriefingAnalysisClient.BriefingResult): BriefingAnalysisClient.Result =
        BriefingAnalysisClient.Result(results.toList())

    private fun briefingResult(target: BriefingAnalysisTask.Context.Target): BriefingAnalysisClient.BriefingResult =
        BriefingAnalysisClient.BriefingResult(
            briefingId = target.briefingPublicId,
            agentId = target.agentPublicId,
            agentType = target.agentType,
            direction = BriefingDirection.UP,
            probability = BigDecimal("0.72"),
            headline = "헤드라인",
            summary = "요약",
            personalComment = null,
            commonAnalysis = "공통 분석",
            closingComment = "마무리 의견",
            modelName = target.modelName,
            cached = false,
            personalCached = false,
        )
}
