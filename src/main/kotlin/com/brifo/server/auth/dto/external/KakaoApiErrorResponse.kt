package com.brifo.server.auth.dto.external

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class KakaoApiErrorResponse(
    val code: Int? = null,
    val msg: String? = null,
)
