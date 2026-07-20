package com.brifo.server.auth.dto

data class LogoutRequest(
    val refreshToken: String?,
)
