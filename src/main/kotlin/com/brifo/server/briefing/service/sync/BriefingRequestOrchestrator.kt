package com.brifo.server.briefing.service.sync

import com.brifo.server.briefing.dto.response.CreateBriefingResponse
import com.brifo.server.briefing.service.async.BriefingAnalysisOrchestrator
import com.brifo.server.briefing.service.async.BriefingAnalysisTask
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

/** 사용자 브리핑 요청을 검증하고 확정한 뒤 비동기 분석을 제출한다. */
@Service
class BriefingRequestOrchestrator(
    private val validator: BriefingRequestValidator,
    private val transactionService: BriefingRequestTransactionService,
    private val analysisOrchestrator: BriefingAnalysisOrchestrator,
    private val clock: Clock,
) {
    fun request(
        userPublicId: UUID,
        stockPublicId: UUID,
        agentPublicIds: List<UUID>,
    ): CreateBriefingResponse {
        val command = BriefingRequestTask.Command(
            userPublicId = userPublicId,
            stockPublicId = stockPublicId,
            agentPublicIds = agentPublicIds,
            requestedAt = LocalDateTime.now(clock),
        )
        validator.validate(command)
        val result = transactionService.request(command)

        analysisOrchestrator.processAsync(
            BriefingAnalysisTask.Command(
                userPublicId = command.userPublicId,
                briefingPublicIds = result.requestedAgents.map { it.briefingId },
            ),
        )

        return result.toResponse()
    }

    private fun BriefingRequestTask.Result.toResponse(): CreateBriefingResponse =
        CreateBriefingResponse(
            requestedCount = requestedCount,
            totalSalaryCost = totalSalaryCost,
            requestedAgents = requestedAgents.map { agent ->
                CreateBriefingResponse.RequestedAgent(
                    briefingId = agent.briefingId,
                    agentId = agent.agentId,
                    agentType = agent.agentType,
                    salaryCost = agent.salaryCost,
                )
            },
        )
}
