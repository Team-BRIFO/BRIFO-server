package com.brifo.server.diary.repository

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
import com.brifo.server.diary.entity.DiaryEntry
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
class DiaryQueryRepositoryJpaTest @Autowired constructor(
    private val repository: DiaryEntryRepository,
    private val entityManager: EntityManager,
) {
    @Test
    fun `목록은 타 사용자와 미정산 결정을 제외하고 다중 카드를 중복 집계하지 않는다`() {
        val scenario = BriefingDatabaseFixture(entityManager).requestScenario(DATE, agentCount = 2)
        val settled = saveDiary(scenario, scenario.agents[0], settled = true)
        saveDiary(scenario, scenario.agents[1], settled = false)
        val other = BriefingDatabaseFixture(entityManager).requestScenario(DATE, agentCount = 1)
        saveDiary(other, other.agents.single(), settled = true)
        flushAndClear()

        val rows = repository.findDiaryPage(scenario.user.publicId!!, null, 10)

        assertEquals(1, rows.size)
        assertEquals(settled.publicId, rows.single().diaryId)
        assertEquals(scenario.stock.publicId, rows.single().stockId)
        assertEquals(10, rows.single().apDelta)
    }

    @Test
    fun `목록 커서는 해당 일기보다 오래된 결과만 반환한다`() {
        val scenario = BriefingDatabaseFixture(entityManager).requestScenario(DATE, agentCount = 2)
        saveDiary(scenario, scenario.agents[0], settled = true)
        saveDiary(scenario, scenario.agents[1], settled = true)
        flushAndClear()
        val all = repository.findDiaryPage(scenario.user.publicId!!, null, 10)

        val afterCursor = repository.findDiaryPage(scenario.user.publicId!!, all.first().diaryId, 10)

        assertEquals(listOf(all.last().diaryId), afterCursor.map { it.diaryId })
    }

    @Test
    fun `상세 조회는 소유한 정산 일기만 projection 한다`() {
        val owner = BriefingDatabaseFixture(entityManager).requestScenario(DATE, agentCount = 1)
        val diary = saveDiary(owner, owner.agents.single(), settled = true)
        val stranger = BriefingDatabaseFixture(entityManager).requestScenario(DATE, agentCount = 1)
        flushAndClear()

        val detail = repository.findDiaryDetail(owner.user.publicId!!, diary.publicId!!)
        val unauthorized = repository.findDiaryDetail(stranger.user.publicId!!, diary.publicId!!)

        assertEquals(owner.stock.publicId, detail?.stockId)
        assertEquals(BigDecimal("2.55"), detail?.changeRate)
        assertEquals(true, detail?.isCorrect)
        assertNull(unauthorized)
    }

    private fun saveDiary(
        scenario: BriefingDatabaseFixture.RequestScenario,
        agent: com.brifo.server.agent.entity.Agent,
        settled: Boolean,
    ): DiaryEntry {
        val briefing = Briefing.create(scenario.cards, agent).also {
            it.startAnalysis()
            it.complete(BriefingDirection.UP, 72, "분석", "한줄", "제목", "요약", null)
            entityManager.persist(it)
        }
        val decision = Decision.create(briefing, DecisionDirection.UP, 4).also(entityManager::persist)
        val diary = DiaryEntry.create(decision).also(entityManager::persist)
        if (settled) {
            entityManager.flush()
            val price = findOrCreatePrice(scenario)
            entityManager.persist(DecisionResult.create(decision, price, true))
            entityManager.persist(
                ApTransaction.create(
                    user = scenario.user,
                    amount = 10,
                    reason = ApTransactionReason.DECISION_WIN,
                    targetType = ApTransactionTargetType.DECISION,
                    targetId = decision.id!!,
                ),
            )
        }
        entityManager.flush()
        entityManager.refresh(diary)
        return diary
    }

    private fun findOrCreatePrice(scenario: BriefingDatabaseFixture.RequestScenario): DailyStockPrice {
        val existing = entityManager.createQuery(
            "select p from DailyStockPrice p where p.stock = :stock and p.tradeDate = :date",
            DailyStockPrice::class.java,
        ).setParameter("stock", scenario.stock)
            .setParameter("date", DATE)
            .resultList
            .firstOrNull()
        return existing ?: DailyStockPrice.create(
            stock = scenario.stock,
            tradeDate = DATE,
            price = BigDecimal("70000.00"),
            changeRate = BigDecimal("2.55"),
        ).also(entityManager::persist)
    }

    private fun flushAndClear() {
        entityManager.flush()
        entityManager.clear()
    }

    companion object {
        private val DATE = LocalDate.of(2026, 7, 18)
    }
}
