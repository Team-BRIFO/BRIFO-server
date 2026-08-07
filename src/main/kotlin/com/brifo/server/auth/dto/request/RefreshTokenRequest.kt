package com.brifo.server.auth.dto.request

import io.swagger.v3.oas.annotations.media.Schema

data class RefreshTokenRequest(
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val refreshToken: String?,
)
