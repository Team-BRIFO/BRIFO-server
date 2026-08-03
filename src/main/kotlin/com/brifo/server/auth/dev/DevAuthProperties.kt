package com.brifo.server.auth.dev

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties("app.dev-auth")
data class DevAuthProperties(
    val enabled: Boolean = false,
    @field:NotBlank
    val password: String,
)
