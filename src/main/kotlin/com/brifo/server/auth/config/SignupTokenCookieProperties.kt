package com.brifo.server.auth.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.auth.signup-cookie")
data class SignupTokenCookieProperties(
    val secure: Boolean = true,
)
