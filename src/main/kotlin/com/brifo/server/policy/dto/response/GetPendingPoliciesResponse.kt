package com.brifo.server.policy.dto.response

import java.math.BigDecimal
import java.util.UUID

data class GetPendingPoliciesResponse(
    val items: List<PendingPolicyItem>,
) {
    data class PendingPolicyItem(
        val policyId: UUID,
        val title: String,
        val isRequired: Boolean,
        val version: BigDecimal,
    )
}
