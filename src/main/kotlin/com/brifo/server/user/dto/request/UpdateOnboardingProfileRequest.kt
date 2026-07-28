package com.brifo.server.user.dto.request

import java.util.UUID

data class UpdateOnboardingProfileRequest(
    val nickname: String,
    val companyName: String? = null,
    val stockIds: List<UUID>,
)
