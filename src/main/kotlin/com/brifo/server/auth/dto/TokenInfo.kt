package com.brifo.server.auth.dto

data class TokenInfo(
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresIn: Long,
    val refreshTokenExpiresIn: Long,
)
