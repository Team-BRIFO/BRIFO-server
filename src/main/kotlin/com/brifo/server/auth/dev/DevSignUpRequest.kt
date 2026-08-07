package com.brifo.server.auth.dev

import jakarta.validation.constraints.NotBlank

class DevSignUpRequest(
    @field:NotBlank
    val password: String,
) {
    override fun toString(): String = "DevSignUpRequest(password=******)"
}
