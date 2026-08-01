package com.brifo.server.policy.repository

import com.brifo.server.policy.dto.response.GetPendingPoliciesResponse
import com.brifo.server.policy.dto.response.GetPoliciesResponse

interface PolicyQueryRepository {
    fun findAllActiveWithAgreement(userId: Long): List<GetPoliciesResponse.PolicyItem>

    fun findPendingRequired(userId: Long): List<GetPendingPoliciesResponse.PendingPolicyItem>
}
