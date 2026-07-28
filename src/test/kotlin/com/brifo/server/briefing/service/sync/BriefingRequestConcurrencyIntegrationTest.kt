package com.brifo.server.briefing.service.sync

import com.brifo.server.ServerTestConfiguration
import com.brifo.server.ap.entity.ApTransactionReason
import com.brifo.server.ap.repository.ApTransactionRepository
import com.brifo.server.briefing.exception.BriefingAlreadyRequestedException
import com.brifo.server.briefing.repository.BriefingRepository
import com.brifo.server.briefing.service.sync.BriefingRequestTask
import com.brifo.server.briefing.service.sync.BriefingRequestTransactionService
import com.brifo.server.briefing.support.BriefingDatabaseFixture
import com.brifo.server.user.repository.UserRepository
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.test.assertEquals
import kotlin.test.assertIs

@Import(ServerTestConfiguration::class)
@ActiveProfiles("test")
@SpringBootTest
class BriefingRequestConcurrencyIntegrationTest @Autowired constructor(
    private val service: BriefingRequestTransactionService,
    private val briefingRepository: BriefingRepository,
    private val userRepository: UserRepository,
    private val apTransactionRepository: ApTransactionRepository,
    private val entityManager: EntityManager,
    private val transactionTemplate: TransactionTemplate,
) {
    @Test
    fun `동일 사용자의 동시 요청은 사용자 락으로 직렬화되어 한 번만 생성하고 차감한다`() {
        val date = LocalDate.now(ZoneId.of("Asia/Seoul"))
        val scenario = transactionTemplate.execute {
            BriefingDatabaseFixture(entityManager).requestScenario(date)
        }!!
        val command = BriefingRequestTask.Command(
            userPublicId = scenario.user.publicId!!,
            stockPublicId = scenario.stock.publicId!!,
            agentPublicIds = scenario.agents.map { it.publicId!! },
            requestedAt = LocalDateTime.of(date, java.time.LocalTime.of(10, 0)),
        )
        val ready = CountDownLatch(2)
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)

        val futures = (1..2).map {
            executor.submit<Result<BriefingRequestTask.Result>> {
                ready.countDown()
                start.await()
                runCatching { service.request(command) }
            }
        }
        ready.await()
        start.countDown()
        val results = futures.map { it.get() }
        executor.shutdown()

        assertEquals(1, results.count { it.isSuccess })
        assertIs<BriefingAlreadyRequestedException>(results.single { it.isFailure }.exceptionOrNull())
        assertEquals(3, briefingRepository.count())
        assertEquals(3, apTransactionRepository.findAll().count { it.reason == ApTransactionReason.SALARY })
        assertEquals(40, userRepository.findByPublicId(command.userPublicId)!!.balanceAp)
    }
}
