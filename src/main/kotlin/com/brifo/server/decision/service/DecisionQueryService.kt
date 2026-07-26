package com.brifo.server.decision.service

import com.brifo.server.decision.dto.response.GetDecisionResultResponse
import com.brifo.server.decision.dto.response.GetDecisionsResponse
import com.brifo.server.decision.exception.DecisionNotFoundException
import com.brifo.server.decision.exception.DecisionNotSettledException
import com.brifo.server.decision.repository.DecisionRepository
import com.brifo.server.decision.repository.DecisionResultRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

/** 오늘의 미정산 결정과 정산 완료 결과를 조회한다. */
@Service
class DecisionQueryService(
    private val decisionRepository: DecisionRepository,
    private val decisionResultRepository: DecisionResultRepository,
    private val clock: Clock,
) {
    @Transactional(readOnly = true)
    fun getDecisions(userPublicId: UUID): GetDecisionsResponse =
        GetDecisionsResponse(
            items = decisionRepository.findTodayUnsettledDecisions(
                userPublicId = userPublicId,
                displayDate = LocalDate.now(clock),
            ),
        )

    @Transactional(readOnly = true)
    fun getDecisionResult(
        userPublicId: UUID,
        decisionPublicId: UUID,
    ): GetDecisionResultResponse {
        if (
            !decisionRepository.existsByPublicIdAndBriefingAgentUserPublicId(
                decisionPublicId,
                userPublicId,
            )
        ) {
            throw DecisionNotFoundException()
        }
        if (!decisionResultRepository.existsByDecisionPublicId(decisionPublicId)) {
            throw DecisionNotSettledException()
        }
        val results = decisionRepository.findDecisionResults(userPublicId, decisionPublicId)
        check(results.size == 1) {
            "Decision settlement must have exactly one AP transaction: $decisionPublicId"
        }
        return results.single()
    }
}
