package com.brifo.server.briefing.dto.request

import jakarta.validation.constraints.Size
import java.util.UUID

data class CreateBriefingRequest(
    @field:Size(min = 1, max = 3)
    val agentIds: List<UUID>,
)
