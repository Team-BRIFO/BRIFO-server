package com.brifo.server.user.dto.request

import jakarta.validation.constraints.Size
import java.util.UUID

data class UpdateOnboardingProfileRequest(
    val nickname: String,
    val companyName: String? = null,
    @field:Size(min = 1, max = 3)
    val stockIds: List<UUID>,
)
