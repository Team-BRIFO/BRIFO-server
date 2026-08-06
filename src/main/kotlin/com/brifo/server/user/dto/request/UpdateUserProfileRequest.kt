package com.brifo.server.user.dto.request

import jakarta.validation.constraints.Size
import java.util.UUID

data class UpdateUserProfileRequest(
    val nickname: String,
    val companyName: String,
    @field:Size(min = 1, max = 3)
    val stockIds: List<UUID>,
)
