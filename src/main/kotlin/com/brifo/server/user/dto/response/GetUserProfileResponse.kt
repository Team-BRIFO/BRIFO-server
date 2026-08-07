package com.brifo.server.user.dto.response

import io.swagger.v3.oas.annotations.media.ArraySchema
import java.util.UUID

data class GetUserProfileResponse(
    val nickname: String,
    val companyName: String,
    @field:ArraySchema(minItems = 1, maxItems = 3)
    val stocks: List<UserProfileStock>,
) {
    data class UserProfileStock(
        val stockId: UUID,
        val name: String,
        val logoUrl: String?,
    )
}
