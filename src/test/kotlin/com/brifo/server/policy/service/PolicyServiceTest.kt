package com.brifo.server.policy.service

import com.brifo.server.policy.entity.Policy
import com.brifo.server.policy.exception.DuplicatedPolicyIdsException
import com.brifo.server.policy.exception.PolicyNotFoundException
import com.brifo.server.policy.exception.RequiredPolicyCannotBeRevokedException
import com.brifo.server.policy.exception.RequiredPolicyMissingException
import com.brifo.server.policy.repository.PolicyRepository
import com.brifo.server.policy.repository.UserPolicyRepository
import com.brifo.server.user.entity.User
import com.brifo.server.user.exception.UserNotFoundException
import com.brifo.server.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

class PolicyServiceTest {
    private lateinit var policyRepository: PolicyRepository
    private lateinit var userPolicyRepository: UserPolicyRepository
    private lateinit var userRepository: UserRepository
    private lateinit var policyService: PolicyService

    @BeforeEach
    fun setUp() {
        policyRepository = mock(PolicyRepository::class.java)
        userPolicyRepository = mock(UserPolicyRepository::class.java)
        userRepository = mock(UserRepository::class.java)
        policyService = PolicyService(policyRepository, userPolicyRepository, userRepository)
    }

    @Test
    fun `상세 조회는 활성 약관만 반환한다`() {
        val policyId = UUID.randomUUID()
        val createdAt = LocalDateTime.of(2026, 7, 20, 10, 0)
        val policy = policy(11L, policyId, "서비스 이용약관", createdAt = createdAt)
        `when`(policyRepository.findByPublicIdAndIsActiveTrue(policyId))
            .thenReturn(policy)

        val response = policyService.getPolicyDetail(policyId)

        assertEquals(policyId, response.policyId)
        assertEquals("서비스 이용약관", response.title)
        assertEquals(createdAt, response.createdAt)
    }

    @Test
    fun `비활성 또는 존재하지 않는 약관 상세 조회는 POLICY_404 예외를 던진다`() {
        val policyId = UUID.randomUUID()
        `when`(policyRepository.findByPublicIdAndIsActiveTrue(policyId)).thenReturn(null)

        val exception = assertThrows(PolicyNotFoundException::class.java) {
            policyService.getPolicyDetail(policyId)
        }

        assertEquals("POLICY_404", exception.errorCode.code)
    }

    @Test
    fun `존재하지 않는 사용자의 약관 요청은 USER_404 예외를 던진다`() {
        val userId = UUID.randomUUID()
        `when`(userRepository.findByPublicId(userId)).thenReturn(null)

        val exception = assertThrows(UserNotFoundException::class.java) {
            policyService.getPolicies(userId)
        }

        assertEquals("USER_404", exception.errorCode.code)
    }

    @Test
    fun `중복 약관 ID 요청은 COMMON_400 예외를 던진다`() {
        val policyId = UUID.randomUUID()
        val exception = assertThrows(DuplicatedPolicyIdsException::class.java) {
            policyService.agreePolicies(UUID.randomUUID(), listOf(policyId, policyId))
        }
        assertEquals("COMMON_400", exception.errorCode.code)
        verifyNoInteractions(userRepository)
    }

    @Test
    fun `존재하지 않거나 비활성인 요청 약관이 있으면 요청 전체를 실패시킨다`() {
        val userId = UUID.randomUUID()
        val firstPolicyId = UUID.randomUUID()
        val missingPolicyId = UUID.randomUUID()
        givenUser(userId, 7L)
        val requestedPolicy = policy(11L, firstPolicyId, "필수")
        `when`(policyRepository.findAllByPublicIdInAndIsActiveTrue(listOf(firstPolicyId, missingPolicyId)))
            .thenReturn(listOf(requestedPolicy))
        val exception = assertThrows(PolicyNotFoundException::class.java) {
            policyService.agreePolicies(userId, listOf(firstPolicyId, missingPolicyId))
        }
        assertEquals("POLICY_404", exception.errorCode.code)
        verifyNoInteractions(userPolicyRepository)
    }

    @Test
    fun `기존 동의와 요청을 합쳐 활성 필수 약관을 모두 충족하면 동의한다`() {
        val userId = UUID.randomUUID()
        val requiredTwo = policy(12L, UUID.randomUUID(), "필수 2")
        val optional = policy(13L, UUID.randomUUID(), "선택", isRequired = false)
        val requiredOne = policy(11L, UUID.randomUUID(), "필수 1")
        givenUser(userId, 7L)
        val requestedPublicIds = listOf(requireNotNull(requiredTwo.publicId), requireNotNull(optional.publicId))
        `when`(policyRepository.findAllByPublicIdInAndIsActiveTrue(requestedPublicIds))
            .thenReturn(listOf(requiredTwo, optional))
        `when`(userPolicyRepository.findActivePolicyIdsByUserId(7L)).thenReturn(setOf(11L))
        `when`(policyRepository.findAllByIsActiveTrueAndIsRequiredTrue()).thenReturn(listOf(requiredOne, requiredTwo))
        policyService.agreePolicies(userId, requestedPublicIds)
        verify(userPolicyRepository).insertActiveIfAbsent(7L, 12L)
        verify(userPolicyRepository).insertActiveIfAbsent(7L, 13L)
    }

    @Test
    fun `활성 필수 약관이 누락되면 어떤 동의도 저장하지 않는다`() {
        val userId = UUID.randomUUID()
        val optional = policy(12L, UUID.randomUUID(), "선택", isRequired = false)
        val required = policy(11L, UUID.randomUUID(), "필수")
        givenUser(userId, 7L)
        `when`(policyRepository.findAllByPublicIdInAndIsActiveTrue(listOf(requireNotNull(optional.publicId))))
            .thenReturn(listOf(optional))
        `when`(userPolicyRepository.findActivePolicyIdsByUserId(7L)).thenReturn(emptySet())
        `when`(policyRepository.findAllByIsActiveTrueAndIsRequiredTrue())
            .thenReturn(listOf(required))
        val exception = assertThrows(RequiredPolicyMissingException::class.java) {
            policyService.agreePolicies(userId, listOf(requireNotNull(optional.publicId)))
        }
        assertEquals("POLICY_400_01", exception.errorCode.code)
        verify(userPolicyRepository, never()).insertActiveIfAbsent(anyLong(), anyLong())
    }

    @Test
    fun `활성 필수 약관이 없으면 선택 약관만 동의할 수 있다`() {
        val userId = UUID.randomUUID()
        val optional = policy(12L, UUID.randomUUID(), "선택", isRequired = false)
        givenUser(userId, 7L)
        `when`(policyRepository.findAllByPublicIdInAndIsActiveTrue(listOf(requireNotNull(optional.publicId))))
            .thenReturn(listOf(optional))
        `when`(userPolicyRepository.findActivePolicyIdsByUserId(7L)).thenReturn(emptySet())
        `when`(policyRepository.findAllByIsActiveTrueAndIsRequiredTrue()).thenReturn(emptyList())
        policyService.agreePolicies(userId, listOf(requireNotNull(optional.publicId)))
        verify(userPolicyRepository).insertActiveIfAbsent(7L, 12L)
    }

    @Test
    fun `필수 약관은 철회할 수 없다`() {
        val userId = UUID.randomUUID()
        val policyId = UUID.randomUUID()
        givenUser(userId, 7L)
        val policy = policy(11L, policyId, "필수", isRequired = true)
        `when`(policyRepository.findByPublicIdAndIsActiveTrue(policyId))
            .thenReturn(policy)
        val exception = assertThrows(RequiredPolicyCannotBeRevokedException::class.java) {
            policyService.revokePolicy(userId, policyId)
        }
        assertEquals("POLICY_400_02", exception.errorCode.code)
        verify(userPolicyRepository, never()).revokeActive(anyLong(), anyLong())
    }

    @Test
    fun `존재하지 않거나 비활성인 약관은 철회할 수 없다`() {
        val userId = UUID.randomUUID()
        val policyId = UUID.randomUUID()
        givenUser(userId, 7L)
        `when`(policyRepository.findByPublicIdAndIsActiveTrue(policyId)).thenReturn(null)
        val exception = assertThrows(PolicyNotFoundException::class.java) {
            policyService.revokePolicy(userId, policyId)
        }

        assertEquals("POLICY_404", exception.errorCode.code)
        verifyNoInteractions(userPolicyRepository)
    }

    @Test
    fun `선택 약관 철회는 활성 동의 갱신을 호출하고 이력이 없어도 성공한다`() {
        val userId = UUID.randomUUID()
        val policyId = UUID.randomUUID()
        givenUser(userId, 7L)
        val policy = policy(11L, policyId, "선택", isRequired = false)
        `when`(policyRepository.findByPublicIdAndIsActiveTrue(policyId))
            .thenReturn(policy)
        `when`(userPolicyRepository.revokeActive(7L, 11L)).thenReturn(0)
        policyService.revokePolicy(userId, policyId)
        verify(userPolicyRepository).revokeActive(7L, 11L)
    }

    private fun givenUser(
        publicId: UUID,
        internalId: Long,
    ) {
        val user = mock(User::class.java)
        `when`(user.id).thenReturn(internalId)
        `when`(userRepository.findByPublicId(publicId)).thenReturn(user)
    }

    private fun policy(
        id: Long,
        publicId: UUID,
        title: String,
        isRequired: Boolean = true,
        createdAt: LocalDateTime = LocalDateTime.of(2026, 7, 20, 10, 0),
    ): Policy =
        mock(Policy::class.java).also {
            `when`(it.id).thenReturn(id)
            `when`(it.publicId).thenReturn(publicId)
            `when`(it.title).thenReturn(title)
            `when`(it.content).thenReturn("약관 전문")
            `when`(it.isRequired).thenReturn(isRequired)
            `when`(it.version).thenReturn(BigDecimal("1.10"))
            `when`(it.createdAt).thenReturn(createdAt)
        }
}
