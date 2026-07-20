package com.brifo.server.auth.dto.request

import jakarta.validation.constraints.NotBlank

data class KakaoLoginRequest(
    @field:NotBlank(message = "인가 코드는 필수입니다.")
    val authorizationCode: String,
    @field:NotBlank(message = "Redirect URI는 필수입니다.")
    val redirectUri: String,
)
