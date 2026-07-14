package com.brifo.server.user.dto.request

import java.util.UUID

data class UpdateUserProfileRequest(
    val nickname: String,
    val companyName: String,
    val stockIds: List<UUID>,
)
