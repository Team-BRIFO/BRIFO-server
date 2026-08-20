package com.brifo.server.decision.service

import com.brifo.server.decision.exception.DecisionNotFoundException
import com.brifo.server.decision.exception.DecisionNotSettledException
import com.brifo.server.decision.repository.DecisionRepository
import com.brifo.server.decision.repository.DecisionResultRepository
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlin.test.assertFailsWith

class DecisionQueryServiceTest {
    private val repository = mock(DecisionRepository::class.java)
    private val resultRepository = mock(DecisionResultRepository::class.java)
    private val service = DecisionQueryService(
        repository,
        resultRepository,
        Clock.fixed(Instant.parse("2026-07-21T05:00:00Z"), ZoneId.of("Asia/Seoul")),
    )

    @Test
    fun `오늘의 결정 목록은 서울 기준 오늘 날짜로 조회한다`() {
        val userId = UUID.randomUUID()
        val today = LocalDate.of(2026, 7, 21)
        `when`(repository.findTodayDecisions(userId, today)).thenReturn(emptyList())

        service.getDecisions(userId)

        verify(repository).findTodayDecisions(userId, today)
    }

    @Test
    fun `소유한 결정이 아니면 결과를 조회할 수 없다`() {
        val userId = UUID.randomUUID()
        val decisionId = UUID.randomUUID()
        `when`(repository.existsByPublicIdAndBriefingAgentUserPublicId(decisionId, userId)).thenReturn(false)

        assertFailsWith<DecisionNotFoundException> {
            service.getDecisionResult(userId, decisionId)
        }
    }

    @Test
    fun `소유한 결정이 정산 전이면 결과를 조회할 수 없다`() {
        val userId = UUID.randomUUID()
        val decisionId = UUID.randomUUID()
        `when`(repository.existsByPublicIdAndBriefingAgentUserPublicId(decisionId, userId)).thenReturn(true)
        `when`(resultRepository.existsByDecisionPublicId(decisionId)).thenReturn(false)

        assertFailsWith<DecisionNotSettledException> {
            service.getDecisionResult(userId, decisionId)
        }
    }
}
