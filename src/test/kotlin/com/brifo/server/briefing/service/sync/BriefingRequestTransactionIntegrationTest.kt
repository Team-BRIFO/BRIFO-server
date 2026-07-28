package com.brifo.server.briefing.service.sync

import com.brifo.server.ServerTestConfiguration
import com.brifo.server.ap.entity.ApTransactionReason
import com.brifo.server.ap.repository.ApTransactionRepository
import com.brifo.server.ap.exception.InsufficientApBalanceException
import com.brifo.server.briefing.entity.BriefingStatus
import com.brifo.server.briefing.exception.BriefingAgentNotInInitialRequestException
import com.brifo.server.briefing.exception.BriefingRetryCooldownException
import com.brifo.server.briefing.repository.BriefingRepository
import com.brifo.server.briefing.service.sync.BriefingRequestTask
import com.brifo.server.briefing.service.sync.BriefingRequestTransactionService
import com.brifo.server.briefing.support.BriefingDatabaseFixture
import com.brifo.server.news.exception.NewsCardNotFoundException
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@Import(ServerTestConfiguration::class)
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class BriefingRequestTransactionIntegrationTest @Autowired constructor(
    private val service: BriefingRequestTransactionService,
    private val briefingRepository: BriefingRepository,
    private val apTransactionRepository: ApTransactionRepository,
    private val entityManager: EntityManager,
) {
    @Test
    fun `관심 종목의 카드뉴스 두 개로 최대 세 브리핑을 원자적으로 생성하고 비용을 차감한다`() {
        val date = LocalDate.now(ZoneId.of("Asia/Seoul"))
        val scenario = BriefingDatabaseFixture(entityManager).requestScenario(date)

        val result = service.request(command(scenario, date))
        entityManager.flush()
        entityManager.clear()

        val dailyBriefings = briefingRepository.findDailyBriefings(
            scenario.user.publicId!!,
            scenario.stock.publicId!!,
            date,
        )
        val salaryTransactions = apTransactionRepository.findAll()
            .filter { it.reason == ApTransactionReason.SALARY && it.targetId in dailyBriefings.map { briefing -> briefing.id } }
        assertEquals(3, result.requestedCount)
        assertEquals(60, result.totalSalaryCost)
        assertEquals(3, dailyBriefings.size)
        assertEquals(listOf(2, 2, 2), dailyBriefings.map { it.newsCards.size })
        assertEquals(3, salaryTransactions.size)
        assertEquals(40, scenario.user.balanceAp)
    }

    @Test
    fun `카드뉴스가 두 개가 아니면 브리핑과 급여 거래를 만들지 않는다`() {
        val date = LocalDate.of(2026, 7, 18)
        val scenario = BriefingDatabaseFixture(entityManager).requestScenario(date, cardCount = 3, agentCount = 1)
        val briefingCount = briefingRepository.count()
        val transactionCount = apTransactionRepository.count()

        assertFailsWith<NewsCardNotFoundException> { service.request(command(scenario, date)) }

        assertEquals(briefingCount, briefingRepository.count())
        assertEquals(transactionCount, apTransactionRepository.count())
        assertEquals(100, scenario.user.balanceAp)
    }

    @Test
    fun `AP가 부족하면 브리핑과 급여 거래를 만들지 않고 잔액을 유지한다`() {
        val date = LocalDate.of(2026, 7, 18)
        val scenario = BriefingDatabaseFixture(entityManager).requestScenario(
            displayDate = date,
            agentCount = 1,
            balanceAp = 9,
        )
        val briefingCount = briefingRepository.count()
        val transactionCount = apTransactionRepository.count()

        assertFailsWith<InsufficientApBalanceException> { service.request(command(scenario, date)) }

        assertEquals(briefingCount, briefingRepository.count())
        assertEquals(transactionCount, apTransactionRepository.count())
        assertEquals(9, scenario.user.balanceAp)
    }

    @Test
    fun `실패 직후 재요청하면 쿨다운 남은 시간을 반환한다`() {
        val date = LocalDate.now()
        val scenario = BriefingDatabaseFixture(entityManager).requestScenario(date, agentCount = 1)
        service.request(command(scenario, date))
        entityManager.flush()

        val briefing = briefingRepository.findDailyBriefings(
            scenario.user.publicId!!,
            scenario.stock.publicId!!,
            date,
        ).single()
        briefing.startAnalysis()
        briefing.fail()
        entityManager.flush()

        val retryAt = briefing.updatedAt!!
        val exception = assertFailsWith<BriefingRetryCooldownException> {
            service.request(command(scenario, retryAt))
        }

        assertEquals(30, exception.retryAfterSeconds)
    }

    @Test
    fun `재요청은 새 브리핑과 급여 거래를 생성하고 기존 실패 브리핑을 보존한다`() {
        val date = LocalDate.now()
        val scenario = BriefingDatabaseFixture(entityManager).requestScenario(date, agentCount = 1)
        service.request(command(scenario, date))
        entityManager.flush()

        val briefing = briefingRepository.findDailyBriefings(
            scenario.user.publicId!!,
            scenario.stock.publicId!!,
            date,
        ).single()
        val failedBriefingId = briefing.publicId
        briefing.startAnalysis()
        briefing.fail()
        entityManager.flush()

        val result = service.request(command(scenario, briefing.updatedAt!!.plusSeconds(30)))
        entityManager.flush()
        entityManager.clear()

        val dailyBriefings = briefingRepository.findDailyBriefings(
            scenario.user.publicId!!,
            scenario.stock.publicId!!,
            date,
        )
        val salaryTransactions = apTransactionRepository.findAll()
            .filter {
                it.reason == ApTransactionReason.SALARY &&
                    it.targetId in dailyBriefings.map { briefing -> briefing.id }
            }
        assertEquals(2, dailyBriefings.size)
        assertEquals(BriefingStatus.FAILED, dailyBriefings.first().status)
        assertEquals(BriefingStatus.PENDING, dailyBriefings.last().status)
        assertEquals(failedBriefingId, dailyBriefings.first().publicId)
        assertEquals(dailyBriefings.last().publicId, result.requestedAgents.single().briefingId)
        assertEquals(2, salaryTransactions.size)
        assertEquals(2, salaryTransactions.map { it.targetId }.distinct().size)
        assertEquals(80, scenario.user.balanceAp)
    }

    @Test
    fun `최초 요청에 없던 사원은 재요청에 추가할 수 없다`() {
        val date = LocalDate.now()
        val scenario = BriefingDatabaseFixture(entityManager).requestScenario(date, agentCount = 2)
        val initialAgentId = scenario.agents.first().publicId!!
        service.request(command(scenario, date, listOf(initialAgentId)))
        entityManager.flush()

        val briefing = briefingRepository.findDailyBriefings(
            scenario.user.publicId!!,
            scenario.stock.publicId!!,
            date,
        ).single()
        briefing.startAnalysis()
        briefing.fail()
        entityManager.flush()

        assertFailsWith<BriefingAgentNotInInitialRequestException> {
            service.request(command(scenario, briefing.updatedAt!!.plusSeconds(30)))
        }
    }

    private fun command(
        scenario: BriefingDatabaseFixture.RequestScenario,
        date: LocalDate,
        agentPublicIds: List<UUID> = scenario.agents.map { it.publicId!! },
    ) = command(scenario, LocalDateTime.of(date, java.time.LocalTime.of(10, 0)), agentPublicIds)

    private fun command(
        scenario: BriefingDatabaseFixture.RequestScenario,
        requestedAt: LocalDateTime,
        agentPublicIds: List<UUID> = scenario.agents.map { it.publicId!! },
    ) = BriefingRequestTask.Command(
        userPublicId = scenario.user.publicId!!,
        stockPublicId = scenario.stock.publicId!!,
        agentPublicIds = agentPublicIds,
        requestedAt = requestedAt,
    )
}
