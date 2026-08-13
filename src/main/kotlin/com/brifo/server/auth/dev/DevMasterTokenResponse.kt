package com.brifo.server.auth.dev

data class DevMasterTokenResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
)
