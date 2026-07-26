package com.brifo.server.auth.dto.external

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class NaverUserResponse(
    val resultcode: String,
    val message: String,
    val response: Profile? = null,
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Profile(
        val id: String,
        val email: String? = null,
        val nickname: String? = null,
    )
}
