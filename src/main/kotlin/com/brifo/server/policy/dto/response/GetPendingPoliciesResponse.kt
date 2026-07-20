package com.brifo.server.policy.dto.response

import java.math.BigDecimal
import java.util.UUID

data class GetPendingPoliciesResponse(
    val items: List<Item>,
) {
    data class Item(
        val policyId: UUID,
        val title: String,
        val isRequired: Boolean,
        val version: BigDecimal,
    )
}
