package com.brifo.server.auth.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class KakaoOAuthErrorResponse(
    val error: String? = null,
    @JsonProperty("error_description")
    val errorDescription: String? = null,
)
