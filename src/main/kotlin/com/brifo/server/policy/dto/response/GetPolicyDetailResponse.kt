package com.brifo.server.policy.dto.response

import com.fasterxml.jackson.annotation.JsonFormat
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class GetPolicyDetailResponse(
    val policyId: UUID,
    val title: String,
    val content: String,
    @field:JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss[.SSSSSS]")
    val createdAt: LocalDateTime,
    val version: BigDecimal,
)
