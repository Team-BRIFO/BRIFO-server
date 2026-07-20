package com.brifo.server.auth.dto.response

sealed interface KakaoLoginResponse {
    val loginType: LoginType

    data class Login(
        override val loginType: LoginType = LoginType.LOGIN,
        val user: UserInfo,
        val token: TokenInfo,
    ) : KakaoLoginResponse

    data class SignupRequired(
        override val loginType: LoginType = LoginType.SIGNUP_REQUIRED,
        val signupToken: String,
    ) : KakaoLoginResponse
}
