package com.brifo.server.user.dto.response

import java.util.UUID

data class GetUserProfileResponse(
    val nickname: String,
    val companyName: String,
    val stocks: List<UserProfileStock>,
) {
    data class UserProfileStock(
        val stockId: UUID,
        val name: String,
    )
}
