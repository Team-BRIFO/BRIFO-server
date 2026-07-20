package com.brifo.server.auth.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class NaverLoginRequest(
    @field:NotBlank(message = "인가 코드는 필수입니다.")
    val authorizationCode: String,
    @field:NotBlank(message = "state는 필수입니다.")
    @field:Size(max = 255, message = "state는 255자 이하여야 합니다.")
    val state: String,
    @field:NotBlank(message = "Redirect URI는 필수입니다.")
    val redirectUri: String,
)
