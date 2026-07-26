package com.brifo.server.ap.repository

import com.brifo.server.TestcontainersConfiguration
import com.brifo.server.ap.entity.ApTransaction
import com.brifo.server.ap.entity.ApTransactionReason
import com.brifo.server.ap.entity.ApTransactionTargetType
import com.brifo.server.global.config.JpaConfig
import com.brifo.server.global.config.QueryDslConfig
import com.brifo.server.user.entity.OAuthProvider
import com.brifo.server.user.entity.User
import com.brifo.server.user.repository.UserRepository
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

@DataJpaTest
@Import(TestcontainersConfiguration::class, JpaConfig::class, QueryDslConfig::class)
@ActiveProfiles("test")
class ApTransactionRepositoryJpaTest {
    @Autowired
    private lateinit var repository: ApTransactionRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @Test
    fun `거래 목록은 해당 사용자의 거래를 publicId 내림차순으로 조회하고 커서 이전 거래만 반환한다`() {
        val user = saveUser("user-1")
        val otherUser = saveUser("user-2")
        val transactions =
            listOf(50, 60, 70).map { amount ->
                saveTransaction(user, amount, ApTransactionReason.ATTENDANCE, ApTransactionTargetType.ATTENDANCE_REWARD, amount.toLong())
            }
        saveTransaction(otherUser, 80, ApTransactionReason.ATTENDANCE, ApTransactionTargetType.ATTENDANCE_REWARD, 80L)
        val newestFirst = transactions.sortedByDescending { requireNotNull(it.publicId) }
        entityManager.clear()

        val firstPage = repository.findPageByUserId(requireNotNull(user.id), null, 2)
        val afterCursor =
            repository.findPageByUserId(
                requireNotNull(user.id),
                requireNotNull(newestFirst.first().publicId),
                10,
            )

        assertEquals(newestFirst.take(2).map { it.publicId }, firstPage.map { it.apTransactionId })
        assertEquals(newestFirst.drop(1).map { it.publicId }, afterCursor.map { it.apTransactionId })
        assertEquals(listOf(60, 50).sortedDescending(), afterCursor.map { it.amount }.sortedDescending())
    }

    @Test
    fun `월 요약은 급여 환불을 획득에서 제외하고 환불된 급여를 손실에서 제외한다`() {
        val user = saveUser("summary-user")
        user.changeAp(300)
        saveTransaction(user, 300, ApTransactionReason.TUTORIAL)
        user.changeAp(-40)
        saveTransaction(user, -40, ApTransactionReason.SALARY, ApTransactionTargetType.BRIEFING, 91L)
        user.changeAp(10)
        saveTransaction(user, 10, ApTransactionReason.SALARY_REFUND, ApTransactionTargetType.BRIEFING, 91L)
        user.changeAp(-20)
        saveTransaction(user, -20, ApTransactionReason.DECISION_LOSE, ApTransactionTargetType.DECISION, 92L)
        entityManager.flush()
        entityManager.clear()
        val today = LocalDate.now()

        val summary =
            repository.findMonthlyAmountsByUserId(
                requireNotNull(user.id),
                today.withDayOfMonth(1).atStartOfDay(),
                today.plusMonths(1).withDayOfMonth(1).atStartOfDay(),
            )

        assertEquals(300, summary.earnedAp)
        assertEquals(20, summary.lostAp)
    }

    @Test
    fun `거래가 없는 사용자의 월 합계는 0이다`() {
        val user = saveUser("empty-user")
        val today = LocalDate.now()

        val summary =
            repository.findMonthlyAmountsByUserId(
                requireNotNull(user.id),
                today.withDayOfMonth(1).atStartOfDay(),
                today.plusMonths(1).withDayOfMonth(1).atStartOfDay(),
            )

        assertEquals(0, summary.earnedAp)
        assertEquals(0, summary.lostAp)
    }

    private fun saveUser(socialId: String): User =
        userRepository.saveAndFlush(
            User.create(
                provider = OAuthProvider.KAKAO,
                socialId = socialId,
                email = "$socialId@example.com",
            ),
        ).also(entityManager::refresh)

    private fun saveTransaction(
        user: User,
        amount: Int,
        reason: ApTransactionReason,
        targetType: ApTransactionTargetType? = null,
        targetId: Long? = null,
    ): ApTransaction =
        repository.saveAndFlush(
            ApTransaction.create(user, amount, reason, targetType, targetId),
        ).also(entityManager::refresh)
}
