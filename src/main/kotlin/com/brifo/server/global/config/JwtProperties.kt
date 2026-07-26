package com.brifo.server.global.config

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.time.Duration

@Validated
@ConfigurationProperties("app.auth.jwt")
data class JwtProperties(
    @field:NotBlank
    val secretBase64: String,
    @field:NotBlank
    val issuer: String = "brifo",
    val accessTokenExpiration: Duration = Duration.ofHours(1),
    val refreshTokenExpiration: Duration = Duration.ofDays(7),
    val signupTokenExpiration: Duration = Duration.ofMinutes(10),
) {
    init {
        require(!accessTokenExpiration.isNegative && !accessTokenExpiration.isZero)
        require(!refreshTokenExpiration.isNegative && !refreshTokenExpiration.isZero)
        require(!signupTokenExpiration.isNegative && !signupTokenExpiration.isZero)
    }
}
