package com.brifo.server.briefing.repository

import com.brifo.server.ServerTestConfiguration
import com.brifo.server.briefing.entity.Briefing
import com.brifo.server.briefing.repository.BriefingRepository
import com.brifo.server.briefing.support.BriefingDatabaseFixture
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import kotlin.test.assertEquals

@Import(ServerTestConfiguration::class)
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class BriefingQueryRepositoryJpaTest @Autowired constructor(
    private val repository: BriefingRepository,
    private val entityManager: EntityManager,
) {
    @Test
    fun `카드뉴스 두 개 조인은 같은 날짜의 브리핑 세 건을 중복 없이 반환한다`() {
        val date = LocalDate.of(2026, 7, 18)
        val scenario = BriefingDatabaseFixture(entityManager).requestScenario(date)
        scenario.agents.forEach { entityManager.persist(Briefing.create(scenario.cards, it)) }
        entityManager.flush()
        entityManager.clear()

        val dailyBriefings = repository.findDailyBriefings(
            scenario.user.publicId!!,
            scenario.stock.publicId!!,
            date,
        )
        val items = repository.findStockBriefingItems(
            scenario.user.publicId!!,
            scenario.stock.publicId!!,
            date,
        )
        val office = repository.findOfficeBriefings(scenario.user.publicId!!, date)

        assertEquals(3, dailyBriefings.size)
        assertEquals(3, dailyBriefings.map { it.publicId }.distinct().size)
        assertEquals(listOf(2, 2, 2), dailyBriefings.map { it.newsCards.size })
        assertEquals(3, items.size)
        assertEquals(1, office.size)
        assertEquals(3, office.single().agents.size)
    }

    @Test
    fun `사용자 종목 표시일 중 하나라도 다르면 일일 브리핑 조회에서 제외한다`() {
        val date = LocalDate.of(2026, 7, 18)
        val today = BriefingDatabaseFixture(entityManager).requestScenario(date, agentCount = 1)
        val otherDate = BriefingDatabaseFixture(entityManager).requestScenario(date.minusDays(1), agentCount = 1)
        entityManager.persist(Briefing.create(today.cards, today.agents.single()))
        entityManager.persist(Briefing.create(otherDate.cards, otherDate.agents.single()))
        entityManager.flush()
        entityManager.clear()

        val result = repository.findDailyBriefings(
            today.user.publicId!!,
            today.stock.publicId!!,
            date,
        )

        assertEquals(1, result.size)
        assertEquals(today.agents.single().publicId, result.single().agent.publicId)
    }
}
