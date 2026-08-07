package com.brifo.server.auth.dto.response

import com.fasterxml.jackson.annotation.JsonIgnore

sealed interface OAuthLoginResponse {
    val loginType: LoginType

    enum class LoginType {
        LOGIN,
        SIGNUP_REQUIRED,
    }

    data class Login(
        override val loginType: LoginType = LoginType.LOGIN,
        val user: UserInfo,
        val token: TokenInfo,
    ) : OAuthLoginResponse

    data class SignupRequired(
        override val loginType: LoginType = LoginType.SIGNUP_REQUIRED,
        @field:JsonIgnore
        val signupToken: String,
    ) : OAuthLoginResponse
}
