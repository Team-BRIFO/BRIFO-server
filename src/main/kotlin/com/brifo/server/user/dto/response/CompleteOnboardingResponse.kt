package com.brifo.server.user.dto.response

data class CompleteOnboardingResponse(
    val token: Token,
) {
    data class Token(
        val accessToken: String,
        val refreshToken: String,
        val accessTokenExpiresIn: Long,
        val refreshTokenExpiresIn: Long,
    )
}
