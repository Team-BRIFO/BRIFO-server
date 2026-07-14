package com.brifo.server.policy.dto.response

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class GetPolicyDetailResponse(
    val policyId: UUID,
    val title: String,
    val content: String,
    val createdAt: LocalDateTime,
    val version: BigDecimal,
)
