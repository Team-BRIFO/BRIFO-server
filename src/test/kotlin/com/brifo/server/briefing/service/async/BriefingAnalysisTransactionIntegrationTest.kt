package com.brifo.server.briefing.service.async

import com.brifo.server.ServerTestConfiguration
import com.brifo.server.ap.entity.ApTransaction
import com.brifo.server.ap.repository.ApTransactionRepository
import com.brifo.server.briefing.entity.Briefing
import com.brifo.server.briefing.entity.BriefingDirection
import com.brifo.server.briefing.entity.BriefingStatus
import com.brifo.server.briefing.repository.BriefingRepository
import com.brifo.server.briefing.support.BriefingDatabaseFixture
import com.brifo.server.user.repository.UserRepository
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals

@Import(ServerTestConfiguration::class)
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class BriefingAnalysisTransactionIntegrationTest @Autowired constructor(
    private val service: BriefingAnalysisTransactionService,
    private val briefingRepository: BriefingRepository,
    private val userRepository: UserRepository,
    private val apTransactionRepository: ApTransactionRepository,
    private val entityManager: EntityManager,
) {
    @Test
    fun `분석 시작과 완료는 같은 브리핑을 분석 중에서 완료 상태로 전이한다`() {
        val prepared = prepareBriefing()

        val context = service.start(
            BriefingAnalysisTask.Command(prepared.userPublicId, listOf(prepared.briefingPublicId)),
        )
        service.complete(completion(prepared.briefingPublicId))
        entityManager.flush()
        entityManager.clear()

        val briefing = briefingRepository.findByPublicId(prepared.briefingPublicId)!!
        assertEquals(listOf(prepared.briefingPublicId), context!!.targets.map { it.briefingPublicId })
        assertEquals(2, context.newsCardPublicIds.size)
        assertEquals(BriefingStatus.COMPLETED, briefing.status)
        assertEquals(BriefingDirection.UP, briefing.direction)
        assertEquals(72, briefing.confidenceRate)
    }

    @Test
    fun `최종 실패는 상태 환불 거래와 사용자 잔액을 함께 반영하고 중복 호출은 무시한다`() {
        val prepared = prepareBriefing()
        service.start(BriefingAnalysisTask.Command(prepared.userPublicId, listOf(prepared.briefingPublicId)))

        service.failAndRefund(prepared.briefingPublicId)
        service.failAndRefund(prepared.briefingPublicId)
        entityManager.flush()
        entityManager.clear()

        val briefing = briefingRepository.findByPublicId(prepared.briefingPublicId)!!
        val user = userRepository.findByPublicId(prepared.userPublicId)!!
        assertEquals(BriefingStatus.FAILED, briefing.status)
        assertEquals(100, user.balanceAp)
        assertEquals(0, apTransactionRepository.sumBriefingSalaryBalance(briefing.id!!))
        assertEquals(2, apTransactionRepository.findAll().count { it.targetId == briefing.id })
    }

    private fun prepareBriefing(): PreparedBriefing {
        val scenario = BriefingDatabaseFixture(entityManager).requestScenario(
            LocalDate.of(2026, 7, 18),
            agentCount = 1,
        )
        val briefing = Briefing.create(scenario.cards, scenario.agents.single())
        entityManager.persist(briefing)
        entityManager.flush()
        scenario.user.spendAp(10)
        entityManager.persist(ApTransaction.salary(scenario.user, briefing.id!!, 10))
        entityManager.flush()
        return PreparedBriefing(scenario.user.publicId!!, briefing.publicId!!)
    }

    private fun completion(briefingPublicId: java.util.UUID) =
        BriefingAnalysisTask.Completion(
            briefingPublicId = briefingPublicId,
            direction = BriefingDirection.UP,
            probability = BigDecimal("0.72"),
            headline = "헤드라인",
            summary = "요약",
            personalComment = null,
            commonAnalysis = "분석",
            closingComment = "의견",
        )

    private data class PreparedBriefing(
        val userPublicId: java.util.UUID,
        val briefingPublicId: java.util.UUID,
    )
}
