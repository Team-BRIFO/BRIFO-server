package com.brifo.server.agent.service

import com.brifo.server.agent.entity.AgentType
import com.brifo.server.agent.exception.AgentNotFoundException
import com.brifo.server.agent.repository.AgentQueryRepository
import com.brifo.server.agent.repository.AgentRepository
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AgentServiceTest {
    private val repository = mock(AgentRepository::class.java)
    private val service = AgentService(
        repository,
        Clock.fixed(Instant.parse("2026-07-23T03:00:00Z"), ZoneId.of("Asia/Seoul")),
    )

    @Test
    fun `적중률은 정산 완료 분석 비율을 반올림하고 이력이 없으면 0이다`() {
        val userId = UUID.randomUUID()
        `when`(repository.findAgentSummaries(userId)).thenReturn(
            listOf(
                summary(total = 3, correct = 2),
                summary(total = 0, correct = 0),
            ),
        )

        assertEquals(listOf(67, 0), service.getAgents(userId).items.map { it.accuracyRate })
    }

    @Test
    fun `연속 근무일은 오늘부터 빈날 전까지만 계산한다`() {
        val userId = UUID.randomUUID()
        val publicId = UUID.randomUUID()
        `when`(repository.findAgentDetail(userId, publicId)).thenReturn(detail(publicId))
        `when`(repository.findCompletedWorkDates(7L)).thenReturn(
            listOf(
                LocalDate.of(2026, 7, 23),
                LocalDate.of(2026, 7, 22),
                LocalDate.of(2026, 7, 21),
                LocalDate.of(2026, 7, 19),
            ),
        )

        val result = service.getAgentDetail(userId, publicId)

        assertEquals(3, result.consecutiveWorkDays)
        assertEquals(560, result.contributedAp)
        assertEquals(75, result.accuracyRate)
    }

    @Test
    fun `오늘 완료 이력이 없으면 어제 이력과 무관하게 연속 근무일은 0이다`() {
        val userId = UUID.randomUUID()
        val publicId = UUID.randomUUID()
        `when`(repository.findAgentDetail(userId, publicId)).thenReturn(detail(publicId))
        `when`(repository.findCompletedWorkDates(7L)).thenReturn(listOf(LocalDate.of(2026, 7, 22)))

        assertEquals(0, service.getAgentDetail(userId, publicId).consecutiveWorkDays)
    }

    @Test
    fun `소유하지 않은 사원은 AGENT_404를 발생시킨다`() {
        val userId = UUID.randomUUID()
        val agentId = UUID.randomUUID()
        `when`(repository.findAgentDetail(userId, agentId)).thenReturn(null)

        assertFailsWith<AgentNotFoundException> {
            service.getAgentDetail(userId, agentId)
        }
    }

    private fun summary(
        total: Long,
        correct: Long,
    ) = AgentQueryRepository.AgentSummary(
        agentId = total + 1,
        publicId = UUID.randomUUID(),
        nickname = "루키",
        agentType = AgentType.ROOKIE,
        modelName = "gpt-4.1-mini",
        level = 1,
        exp = 120,
        dailySalary = 10,
        totalAnalyses = total,
        correctAnalyses = correct,
    )

    private fun detail(publicId: UUID) = AgentQueryRepository.AgentDetail(
        agentId = 7L,
        publicId = publicId,
        nickname = "루키",
        agentType = AgentType.ROOKIE,
        level = 3,
        exp = 145,
        modelName = "gpt-4.1-mini",
        description = "신입 분석가",
        dailySalary = 100,
        totalAnalyses = 4,
        correctAnalyses = 3,
        contributedAp = 560,
    )
}
