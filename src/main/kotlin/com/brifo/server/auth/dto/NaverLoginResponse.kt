package com.brifo.server.auth.dto

sealed interface NaverLoginResponse {
    val loginType: LoginType

    data class Login(
        override val loginType: LoginType = LoginType.LOGIN,
        val user: UserInfo,
        val token: TokenInfo,
    ) : NaverLoginResponse

    data class SignupRequired(
        override val loginType: LoginType = LoginType.SIGNUP_REQUIRED,
        val signupToken: String,
    ) : NaverLoginResponse
}
