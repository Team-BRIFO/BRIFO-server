package com.brifo.server.policy.dto.response

import java.util.UUID

data class GetPoliciesResponse(
    val items: List<PolicyItem>,
) {
    data class PolicyItem(
        val policyId: UUID,
        val title: String,
        val isRequired: Boolean,
        val isAgreed: Boolean,
    )
}
