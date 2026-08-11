package com.brifo.server.decision.repository

import com.brifo.server.ServerTestConfiguration
import com.brifo.server.briefing.entity.Briefing
import com.brifo.server.briefing.entity.BriefingDirection
import com.brifo.server.briefing.support.BriefingDatabaseFixture
import com.brifo.server.decision.entity.Decision
import com.brifo.server.decision.entity.DecisionDirection
import com.brifo.server.decision.entity.DecisionResult
import com.brifo.server.stock.entity.DailyStockPrice
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Import(ServerTestConfiguration::class)
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class DecisionQueryRepositoryJpaTest @Autowired constructor(
    private val decisionRepository: DecisionRepository,
    private val entityManager: EntityManager,
) {
    @Test
    fun `사용자 종목 displayDate가 모두 일치하는 결정만 존재한다고 판단한다`() {
        val date = LocalDate.of(2026, 7, 21)
        val scenario = BriefingDatabaseFixture(entityManager).requestScenario(date, agentCount = 1)
        persistDecision(scenario, DecisionDirection.UP)
        entityManager.flush()

        assertTrue(
            decisionRepository.existsDailyDecision(
                scenario.user.publicId!!,
                scenario.stock.publicId!!,
                date,
            ),
        )
        assertFalse(
            decisionRepository.existsDailyDecision(
                UUID.randomUUID(),
                scenario.stock.publicId!!,
                date,
            ),
        )
        assertFalse(
            decisionRepository.existsDailyDecision(
                scenario.user.publicId!!,
                UUID.randomUUID(),
                date,
            ),
        )
        assertFalse(
            decisionRepository.existsDailyDecision(
                scenario.user.publicId!!,
                scenario.stock.publicId!!,
                date.minusDays(1),
            ),
        )
    }

    @Test
    fun `오늘의 미정산 결정은 최신 가격이 없어도 중복 없이 조회한다`() {
        val date = LocalDate.of(2026, 7, 21)
        val scenario = BriefingDatabaseFixture(entityManager).requestScenario(date, agentCount = 1)
        val decision = persistDecision(scenario, DecisionDirection.DOWN)
        entityManager.flush()
        entityManager.clear()

        val items = decisionRepository.findTodayUnsettledDecisions(
            scenario.user.publicId!!,
            date,
        )

        assertEquals(1, items.size)
        assertEquals(decision.publicId, items.single().decisionId)
        assertEquals(scenario.agents.single().publicId, items.single().agent.agentId)
        assertNull(items.single().stock.price)
        assertNull(items.single().stock.changeRate)
        assertNull(items.single().stock.tradeDate)
    }

    @Test
    fun `오늘의 미정산 결정은 가장 최신 거래일 시세를 반환한다`() {
        val date = LocalDate.of(2026, 7, 21)
        val scenario = BriefingDatabaseFixture(entityManager).requestScenario(date, agentCount = 1)
        persistDecision(scenario, DecisionDirection.UP)
        DailyStockPrice.create(
            stock = scenario.stock,
            tradeDate = date.minusDays(1),
            price = BigDecimal("71000.00"),
            changeRate = BigDecimal("1.14"),
        ).also(entityManager::persist)
        DailyStockPrice.create(
            stock = scenario.stock,
            tradeDate = date,
            price = BigDecimal("72420.00"),
            changeRate = BigDecimal("2.06"),
        ).also(entityManager::persist)
        entityManager.flush()
        entityManager.clear()

        val item = decisionRepository.findTodayUnsettledDecisions(
            scenario.user.publicId!!,
            date,
        ).single()

        assertEquals(date, item.stock.tradeDate)
        assertEquals(72420L, item.stock.price)
        assertEquals(BigDecimal("2.1"), item.stock.changeRate)
    }

    @Test
    fun `오늘의 내 미정산 결정만 조회한다`() {
        val date = LocalDate.of(2026, 7, 21)
        val scenario = BriefingDatabaseFixture(entityManager).requestScenario(date, agentCount = 1)
        val decision = persistDecision(scenario, DecisionDirection.DOWN)
        entityManager.flush()

        assertTrue(
            decisionRepository.findTodayUnsettledDecisions(UUID.randomUUID(), date).isEmpty(),
        )
        assertTrue(
            decisionRepository.findTodayUnsettledDecisions(
                scenario.user.publicId!!,
                date.minusDays(1),
            ).isEmpty(),
        )

        val price = DailyStockPrice.create(
            stock = scenario.stock,
            tradeDate = date,
            price = BigDecimal("72420.00"),
            changeRate = BigDecimal("2.06"),
        ).also(entityManager::persist)
        DecisionResult.create(decision, price, isCorrect = true).also(entityManager::persist)
        entityManager.flush()
        entityManager.clear()

        assertTrue(
            decisionRepository.findTodayUnsettledDecisions(
                scenario.user.publicId!!,
                date,
            ).isEmpty(),
        )
    }

    @Test
    fun `정산 대상은 장 마감 전에 등록된 결정만 포함한다`() {
        val date = LocalDate.of(2026, 8, 3)
        val beforeCloseScenario = BriefingDatabaseFixture(entityManager).requestScenario(date, agentCount = 1)
        val atCloseScenario = BriefingDatabaseFixture(entityManager).requestScenario(date, agentCount = 1)
        val beforeClose = persistDecision(beforeCloseScenario, DecisionDirection.UP)
        val atClose = persistDecision(atCloseScenario, DecisionDirection.DOWN)
        entityManager.flush()

        updateCreatedAt(requireNotNull(beforeClose.id), date.atTime(15, 29, 59))
        updateCreatedAt(requireNotNull(atClose.id), date.atTime(15, 30))
        entityManager.clear()

        assertEquals(listOf(requireNotNull(beforeClose.id)), decisionRepository.findUnsettledIds(date))
        assertEquals(
            listOf(requireNotNull(beforeCloseScenario.stock.id)),
            decisionRepository.findUnsettledStockIds(date),
        )
        assertEquals(
            SettlementDecision(DecisionDirection.UP, requireNotNull(beforeCloseScenario.stock.id)),
            decisionRepository.findSettlementCandidate(requireNotNull(beforeClose.id)),
        )
    }

    private fun updateCreatedAt(
        decisionId: Long,
        createdAt: LocalDateTime,
    ) {
        entityManager.createNativeQuery("UPDATE decisions SET created_at = :createdAt WHERE id = :decisionId")
            .setParameter("createdAt", createdAt)
            .setParameter("decisionId", decisionId)
            .executeUpdate()
    }

    private fun persistDecision(
        scenario: BriefingDatabaseFixture.RequestScenario,
        direction: DecisionDirection,
    ): Decision {
        val briefing = Briefing.create(
            newsCards = scenario.cards,
            agent = scenario.agents.single(),
        ).also {
            it.startAnalysis()
            it.complete(
                direction = BriefingDirection.UP,
                confidenceRate = 70,
                contentText = "분석 본문",
                oneLiner = "한 줄 결론",
                headline = "분석 제목",
                summary = "분석 요약",
                personalComment = null,
            )
            entityManager.persist(it)
        }
        entityManager.flush()
        return Decision.create(
            briefing = briefing,
            direction = direction,
            confidenceLevel = 4,
        ).also(entityManager::persist)
    }
}
