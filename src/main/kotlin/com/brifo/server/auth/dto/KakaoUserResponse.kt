package com.brifo.server.auth.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class KakaoUserResponse(
    val id: Long,
    @JsonProperty("kakao_account")
    val kakaoAccount: KakaoAccount? = null,
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class KakaoAccount(
        val email: String? = null,
        val profile: Profile? = null,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Profile(
        val nickname: String? = null,
    )
}
