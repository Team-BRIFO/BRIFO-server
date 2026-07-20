package com.brifo.server.user.dto.response

import java.util.UUID

data class GetUserProfileResponse(
    val nickname: String,
    val companyName: String,
    val stocks: List<Stock>,
) {
    data class Stock(
        val stockId: UUID,
        val name: String,
    )
}
