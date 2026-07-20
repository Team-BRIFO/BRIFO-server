package com.brifo.server.user.dto.request

data class UpdateOnboardingProfileRequest(
    val nickname: String,
    val companyName: String? = null,
)
