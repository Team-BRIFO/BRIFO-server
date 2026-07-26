package com.brifo.server.policy.service

import com.brifo.server.policy.dto.response.GetPendingPoliciesResponse
import com.brifo.server.policy.dto.response.GetPoliciesResponse
import com.brifo.server.policy.dto.response.GetPolicyDetailResponse
import com.brifo.server.policy.exception.DuplicatedPolicyIdsException
import com.brifo.server.policy.exception.PolicyNotFoundException
import com.brifo.server.policy.exception.RequiredPolicyCannotBeRevokedException
import com.brifo.server.policy.exception.RequiredPolicyMissingException
import com.brifo.server.policy.repository.PolicyRepository
import com.brifo.server.policy.repository.UserPolicyRepository
import com.brifo.server.user.exception.UserNotFoundException
import com.brifo.server.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class PolicyService(
    private val policyRepository: PolicyRepository,
    private val userPolicyRepository: UserPolicyRepository,
    private val userRepository: UserRepository,
) {
    @Transactional(readOnly = true)
    fun getPolicies(userId: UUID): GetPoliciesResponse {
        val user = userRepository.findByPublicId(userId) ?: throw UserNotFoundException()

        return GetPoliciesResponse(
            items = policyRepository.findAllActiveWithAgreement(requireNotNull(user.id)),
        )
    }

    @Transactional(readOnly = true)
    fun getPolicyDetail(policyId: UUID): GetPolicyDetailResponse {
        val policy = policyRepository.findByPublicIdAndIsActiveTrue(policyId) ?: throw PolicyNotFoundException()

        return GetPolicyDetailResponse(
            policyId = requireNotNull(policy.publicId),
            title = policy.title,
            content = policy.content,
            createdAt = requireNotNull(policy.createdAt),
            version = policy.version,
        )
    }

    @Transactional
    fun agreePolicies(
        userId: UUID,
        policyIds: List<UUID>,
    ) {
        if (policyIds.size != policyIds.toSet().size) {
            throw DuplicatedPolicyIdsException()
        }

        val user = userRepository.findByPublicId(userId) ?: throw UserNotFoundException()
        val requestedPolicies = policyRepository.findAllByPublicIdInAndIsActiveTrue(policyIds)
        if (requestedPolicies.size != policyIds.size) {
            throw PolicyNotFoundException()
        }

        val internalUserId = requireNotNull(user.id)
        val agreedPolicyIds = userPolicyRepository.findActivePolicyIdsByUserId(internalUserId)
        val requestedPolicyIds = requestedPolicies.mapTo(mutableSetOf()) { requireNotNull(it.id) }
        val requiredPolicyIds =
            policyRepository
                .findAllByIsActiveTrueAndIsRequiredTrue()
                .mapTo(mutableSetOf()) { requireNotNull(it.id) }

        if (!(agreedPolicyIds + requestedPolicyIds).containsAll(requiredPolicyIds)) {
            throw RequiredPolicyMissingException()
        }

        requestedPolicyIds.forEach { policyId ->
            userPolicyRepository.insertActiveIfAbsent(
                userId = internalUserId,
                policyId = policyId,
            )
        }
    }

    @Transactional(readOnly = true)
    fun getPendingPolicies(userId: UUID): GetPendingPoliciesResponse {
        val user = userRepository.findByPublicId(userId) ?: throw UserNotFoundException()

        return GetPendingPoliciesResponse(
            items = policyRepository.findPendingRequired(requireNotNull(user.id)),
        )
    }

    @Transactional
    fun revokePolicy(
        userId: UUID,
        policyId: UUID,
    ) {
        val user = userRepository.findByPublicId(userId) ?: throw UserNotFoundException()
        val policy = policyRepository.findByPublicIdAndIsActiveTrue(policyId) ?: throw PolicyNotFoundException()
        if (policy.isRequired) {
            throw RequiredPolicyCannotBeRevokedException()
        }

        userPolicyRepository.revokeActive(
            userId = requireNotNull(user.id),
            policyId = requireNotNull(policy.id),
        )
    }

}
