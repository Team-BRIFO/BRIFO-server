package com.brifo.server.agent.repository

import com.brifo.server.TestcontainersConfiguration
import com.brifo.server.ap.entity.ApTransaction
import com.brifo.server.ap.entity.ApTransactionReason
import com.brifo.server.ap.entity.ApTransactionTargetType
import com.brifo.server.briefing.entity.Briefing
import com.brifo.server.briefing.entity.BriefingDirection
import com.brifo.server.briefing.support.BriefingDatabaseFixture
import com.brifo.server.decision.entity.Decision
import com.brifo.server.decision.entity.DecisionDirection
import com.brifo.server.decision.entity.DecisionResult
import com.brifo.server.global.config.JpaConfig
import com.brifo.server.global.config.QueryDslConfig
import com.brifo.server.stock.entity.DailyStockPrice
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull

@DataJpaTest
@Import(TestcontainersConfiguration::class, JpaConfig::class, QueryDslConfig::class)
@ActiveProfiles("test")
class AgentRepositoryJpaTest @Autowired constructor(
    private val repository: AgentRepository,
    private val entityManager: EntityManager,
) {
    @Test
    fun `목록 Projection은 소유 사원만 id 순으로 반환하고 정산 결과를 집계한다`() {
        val scenario = BriefingDatabaseFixture(entityManager).requestScenario(LocalDate.of(2026, 7, 23), agentCount = 2)
        BriefingDatabaseFixture(entityManager).requestScenario(LocalDate.of(2026, 7, 23), agentCount = 1)
        createSettledDecisions(scenario, correctResults = listOf(true, false, true))
        entityManager.flush()
        entityManager.clear()

        val result = repository.findAgentSummaries(scenario.user.publicId!!)

        assertEquals(scenario.agents.map { it.publicId }, result.map { it.publicId })
        assertEquals(3L, result.first().totalAnalyses)
        assertEquals(2L, result.first().correctAnalyses)
        assertEquals(0L, result.last().totalAnalyses)
    }

    @Test
    fun `상세 Projection은 DECISION_WIN만 기여 AP로 합산하고 소유권과 완료 근무일을 필터링한다`() {
        val scenario = BriefingDatabaseFixture(entityManager).requestScenario(LocalDate.of(2026, 7, 23), agentCount = 1)
        val decisions = createSettledDecisions(scenario, correctResults = listOf(true, false))
        entityManager.persist(
            ApTransaction.create(
                user = scenario.user,
                amount = 40,
                reason = ApTransactionReason.DECISION_WIN,
                targetType = ApTransactionTargetType.DECISION,
                targetId = decisions[0].id!!,
            ),
        )
        entityManager.persist(
            ApTransaction.create(
                user = scenario.user,
                amount = 30,
                reason = ApTransactionReason.NEUTRAL_HIT,
                targetType = ApTransactionTargetType.DECISION,
                targetId = decisions[1].id!!,
            ),
        )
        Briefing.create(scenario.cards, scenario.agents.single()).also {
            it.startAnalysis()
            it.fail()
            entityManager.persist(it)
        }
        entityManager.flush()
        entityManager.clear()

        val agent = scenario.agents.single()
        val detail = repository.findAgentDetail(scenario.user.publicId!!, agent.publicId!!)

        assertEquals(2L, detail?.totalAnalyses)
        assertEquals(1L, detail?.correctAnalyses)
        assertEquals(40L, detail?.contributedAp)
        assertEquals(1, repository.findCompletedWorkDates(agent.id!!).size)
        assertNull(repository.findAgentDetail(java.util.UUID.randomUUID(), agent.publicId!!))
    }

    private fun createSettledDecisions(
        scenario: BriefingDatabaseFixture.RequestScenario,
        correctResults: List<Boolean>,
    ): List<Decision> {
        val price = DailyStockPrice.create(
            stock = scenario.stock,
            tradeDate = LocalDate.of(2026, 7, 23),
            price = BigDecimal("70000"),
            changeRate = BigDecimal("1.20"),
        ).also(entityManager::persist)

        return correctResults.map { isCorrect ->
            val briefing = Briefing.create(scenario.cards, scenario.agents.first()).also {
                it.startAnalysis()
                it.complete(
                    direction = BriefingDirection.UP,
                    confidenceRate = 80,
                    contentText = "분석",
                    oneLiner = "한줄",
                    headline = "제목",
                    summary = "요약",
                    personalComment = null,
                )
                entityManager.persist(it)
            }
            val decision = Decision.create(briefing, DecisionDirection.UP, 3).also(entityManager::persist)
            entityManager.persist(DecisionResult.create(decision, price, isCorrect))
            decision
        }
    }
}
