package com.brifo.server.auth.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class KakaoTokenResponse(
    @JsonProperty("access_token")
    val accessToken: String,
)
