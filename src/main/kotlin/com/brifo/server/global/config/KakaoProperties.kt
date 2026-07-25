package com.brifo.server.global.config

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.time.Duration

@Validated
@ConfigurationProperties("app.auth.kakao")
data class KakaoProperties(
    @field:NotBlank
    val clientId: String,
    val clientSecret: String? = null,
    @field:NotEmpty
    val redirectUris: Set<String>,
    val connectTimeout: Duration = Duration.ofSeconds(3),
    val readTimeout: Duration = Duration.ofSeconds(5),
) {
    init {
        require(!connectTimeout.isNegative && !connectTimeout.isZero)
        require(!readTimeout.isNegative && !readTimeout.isZero)
    }
}
