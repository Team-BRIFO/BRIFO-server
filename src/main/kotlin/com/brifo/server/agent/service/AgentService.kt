package com.brifo.server.agent.service

import com.brifo.server.agent.dto.response.GetAgentDetailResponse
import com.brifo.server.agent.dto.response.GetAgentsResponse
import com.brifo.server.agent.exception.AgentNotFoundException
import com.brifo.server.agent.repository.AgentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import kotlin.math.roundToInt

@Service
class AgentService(
    private val agentRepository: AgentRepository,
    private val clock: Clock,
) {
    @Transactional(readOnly = true)
    fun getAgents(userPublicId: UUID): GetAgentsResponse =
        GetAgentsResponse(
            items = agentRepository.findAgentSummaries(userPublicId).map { summary ->
                GetAgentsResponse.AgentItem(
                    agentId = summary.publicId,
                    nickname = summary.nickname,
                    agentType = summary.agentType,
                    modelName = summary.modelName,
                    level = summary.level,
                    exp = summary.exp,
                    dailySalary = summary.dailySalary,
                    accuracyRate = accuracyRate(summary.totalAnalyses, summary.correctAnalyses),
                )
            },
        )

    @Transactional(readOnly = true)
    fun getAgentDetail(
        userPublicId: UUID,
        agentPublicId: UUID,
    ): GetAgentDetailResponse {
        val detail = agentRepository.findAgentDetail(userPublicId, agentPublicId)
            ?: throw AgentNotFoundException()

        return GetAgentDetailResponse(
            agentId = detail.publicId,
            nickname = detail.nickname,
            agentType = detail.agentType,
            level = detail.level,
            exp = detail.exp,
            modelName = detail.modelName,
            description = detail.description,
            accuracyRate = accuracyRate(detail.totalAnalyses, detail.correctAnalyses),
            totalAnalyses = detail.totalAnalyses.toInt(),
            contributedAp = detail.contributedAp.toInt(),
            consecutiveWorkDays = consecutiveWorkDays(
                agentRepository.findCompletedWorkDates(detail.agentId),
                LocalDate.now(clock),
            ),
            dailySalary = detail.dailySalary,
        )
    }

    private fun accuracyRate(
        totalAnalyses: Long,
        correctAnalyses: Long,
    ): Int {
        if (totalAnalyses == 0L) return 0
        return (correctAnalyses * 100.0 / totalAnalyses).roundToInt()
    }

    private fun consecutiveWorkDays(
        workDates: List<LocalDate>,
        today: LocalDate,
    ): Int {
        val dates = workDates.toSet()
        if (today !in dates) return 0

        var count = 0
        var date = today
        while (date in dates) {
            count++
            date = date.minusDays(1)
        }
        return count
    }
}
