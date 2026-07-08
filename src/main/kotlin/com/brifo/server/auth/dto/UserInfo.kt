package com.brifo.server.auth.dto

import java.util.UUID

data class UserInfo(
    val userId: UUID,
    val nickname: String,
    val email: String?,
)
