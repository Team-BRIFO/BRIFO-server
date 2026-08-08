package com.brifo.server.auth.dev

import com.fasterxml.jackson.annotation.JsonIgnore

data class DevSignUpResponse(
    @field:JsonIgnore
    val signupToken: String,
    val csrfToken: String? = null,
)
