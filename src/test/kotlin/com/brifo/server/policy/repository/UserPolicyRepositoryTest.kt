package com.brifo.server.policy.repository

import com.brifo.server.TestcontainersConfiguration
import com.brifo.server.global.config.JpaConfig
import com.brifo.server.global.config.QueryDslConfig
import com.brifo.server.policy.entity.Policy
import com.brifo.server.user.entity.OAuthProvider
import com.brifo.server.user.entity.User
import com.brifo.server.user.repository.UserRepository
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@DataJpaTest
@Import(TestcontainersConfiguration::class, JpaConfig::class, QueryDslConfig::class)
@ActiveProfiles("test")
class UserPolicyRepositoryTest {
    @Autowired
    private lateinit var userPolicyRepository: UserPolicyRepository

    @Autowired
    private lateinit var policyRepository: PolicyRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @Test
    fun `약관 목록과 pending은 사용자의 활성 동의 상태와 필수 여부를 반영한다`() {
        val user = saveUser()
        val agreedRequired = savePolicy(title = "동의한 필수 약관", isRequired = true)
        val pendingRequired = savePolicy(title = "미동의 필수 약관", isRequired = true)
        val optional = savePolicy(title = "선택 약관", isRequired = false)
        val agreedRequiredPublicId = requireNotNull(agreedRequired.publicId)
        val pendingRequiredPublicId = requireNotNull(pendingRequired.publicId)
        val optionalPublicId = requireNotNull(optional.publicId)
        val userId = requireNotNull(user.id)
        userPolicyRepository.insertActiveIfAbsent(userId, requireNotNull(agreedRequired.id))
        entityManager.flush()
        entityManager.clear()

        val items = policyRepository.findAllActiveWithAgreement(userId)
        val itemById = items.associateBy { it.policyId }
        val pending = policyRepository.findPendingRequired(userId)

        assertEquals(items.map { it.policyId }.sorted(), items.map { it.policyId })
        assertTrue(itemById.getValue(agreedRequiredPublicId).isAgreed)
        assertFalse(itemById.getValue(pendingRequiredPublicId).isAgreed)
        assertFalse(itemById.getValue(optionalPublicId).isAgreed)
        assertEquals(listOf(pendingRequiredPublicId), pending.map { it.policyId })
        assertTrue(pending.single().isRequired)
    }

    @Test
    fun `동의와 철회는 멱등이고 철회 후 재동의하면 새 이력을 생성한다`() {
        val user = saveUser()
        val policy = savePolicy(title = "서비스 이용약관", isRequired = true)
        val userId = requireNotNull(user.id)
        val policyId = requireNotNull(policy.id)

        assertEquals(1, userPolicyRepository.insertActiveIfAbsent(userId, policyId))
        assertEquals(0, userPolicyRepository.insertActiveIfAbsent(userId, policyId))
        entityManager.flush()
        entityManager.clear()
        assertEquals(setOf(policyId), userPolicyRepository.findActivePolicyIdsByUserId(userId))

        assertEquals(1, userPolicyRepository.revokeActive(userId, policyId))
        assertEquals(0, userPolicyRepository.revokeActive(userId, policyId))
        entityManager.flush()
        entityManager.clear()

        val revokedItem =
            policyRepository
                .findAllActiveWithAgreement(userId)
                .single { it.policyId == policy.publicId }
        assertFalse(revokedItem.isAgreed)
        assertTrue(userPolicyRepository.findActivePolicyIdsByUserId(userId).isEmpty())

        assertEquals(1, userPolicyRepository.insertActiveIfAbsent(userId, policyId))
        entityManager.flush()
        entityManager.clear()

        val histories = userPolicyRepository.findAll().sortedBy { it.id }
        assertEquals(2, histories.size)
        assertNotNull(histories.first().revokedAt)
        assertEquals(null, histories.last().revokedAt)
        assertEquals(setOf(policyId), userPolicyRepository.findActivePolicyIdsByUserId(userId))
    }

    private fun saveUser(): User =
        userRepository.saveAndFlush(
            User.create(
                provider = OAuthProvider.KAKAO,
                socialId = "policy-test-user",
                email = "policy@example.com",
            ),
        )

    private fun savePolicy(
        title: String,
        isRequired: Boolean,
    ): Policy =
        policyRepository.saveAndFlush(
            Policy.create(
                title = title,
                content = "약관 전문",
                isRequired = isRequired,
            ),
        )
}
