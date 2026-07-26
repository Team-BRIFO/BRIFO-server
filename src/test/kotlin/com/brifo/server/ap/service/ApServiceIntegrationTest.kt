package com.brifo.server.ap.service

import com.brifo.server.ServerTestConfiguration
import com.brifo.server.ap.entity.ApTransactionReason
import com.brifo.server.ap.repository.ApTransactionRepository
import com.brifo.server.ap.repository.AttendanceRewardRepository
import com.brifo.server.user.entity.OAuthProvider
import com.brifo.server.user.entity.User
import com.brifo.server.user.repository.UserRepository
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals

@Import(ServerTestConfiguration::class)
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class ApServiceIntegrationTest @Autowired constructor(
    private val service: ApService,
    private val userRepository: UserRepository,
    private val transactionRepository: ApTransactionRepository,
    private val attendanceRepository: AttendanceRewardRepository,
    private val entityManager: EntityManager,
) {
    @Test
    fun `출석 보상은 생성된 출석 ID를 AP 원장 대상 ID로 저장한다`() {
        val user = saveUser("attendance-user")

        service.createAttendanceReward(requireNotNull(user.publicId))
        entityManager.flush()

        val reward = attendanceRepository.findAll().single()
        val transaction = transactionRepository.findAll().single { it.reason == ApTransactionReason.ATTENDANCE }
        assertEquals(reward.id, transaction.targetId)
    }

    private fun saveUser(socialId: String): User =
        userRepository.saveAndFlush(
            User.create(OAuthProvider.KAKAO, socialId, "$socialId@example.com"),
        ).also(entityManager::refresh)
}
