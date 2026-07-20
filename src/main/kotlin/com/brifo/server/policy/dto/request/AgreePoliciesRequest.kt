package com.brifo.server.policy.dto.request

import jakarta.validation.constraints.Size
import java.util.UUID

data class AgreePoliciesRequest(
    @field:Size(min = 1)
    val policyIds: List<UUID>,
)
