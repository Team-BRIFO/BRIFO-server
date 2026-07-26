package com.brifo.server.auth.dto.internal

import com.brifo.server.user.entity.OAuthProvider

data class OAuthUserProfile(
    val provider: OAuthProvider,
    val socialId: String,
    val email: String?,
    val nickname: String?,
)
