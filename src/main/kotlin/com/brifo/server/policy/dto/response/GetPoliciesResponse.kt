package com.brifo.server.policy.dto.response

import java.util.UUID

data class GetPoliciesResponse(
    val items: List<Item>,
) {
    data class Item(
        val policyId: UUID,
        val title: String,
        val isRequired: Boolean,
        val isAgreed: Boolean,
    )
}
