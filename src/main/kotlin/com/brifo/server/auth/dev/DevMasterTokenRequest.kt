package com.brifo.server.auth.dev

import jakarta.validation.constraints.NotBlank

class DevMasterTokenRequest(
    @field:NotBlank
    val password: String,
) {
    override fun toString(): String = "DevMasterTokenRequest(password=******)"
}
