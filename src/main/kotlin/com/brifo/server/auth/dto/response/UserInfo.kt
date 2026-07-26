package com.brifo.server.auth.dto.response

import java.util.UUID

data class UserInfo(
    val userId: UUID,
    val nickname: String,
    val email: String?,
)
