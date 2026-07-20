package com.brifo.server.auth.dto.request

data class LogoutRequest(
    val refreshToken: String?,
)
