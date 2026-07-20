package com.brifo.server.auth.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class NaverTokenResponse(
    @JsonProperty("access_token")
    val accessToken: String? = null,
    val error: String? = null,
    @JsonProperty("error_description")
    val errorDescription: String? = null,
)
