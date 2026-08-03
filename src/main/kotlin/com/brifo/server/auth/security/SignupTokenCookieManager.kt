package com.brifo.server.auth.security

import com.brifo.server.auth.config.SignupTokenCookieProperties
import com.brifo.server.global.config.JwtProperties
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class SignupTokenCookieManager(
    private val cookieProperties: SignupTokenCookieProperties,
    private val jwtProperties: JwtProperties,
) {
    fun resolve(request: HttpServletRequest): String? =
        request.cookies
            ?.firstOrNull { it.name == COOKIE_NAME }
            ?.value
            ?.takeIf { it.isNotBlank() }

    fun set(
        response: HttpServletResponse,
        signupToken: String,
    ) {
        response.addHeader(
            HttpHeaders.SET_COOKIE,
            cookie(signupToken, jwtProperties.signupTokenExpiration).toString(),
        )
    }

    fun clear(response: HttpServletResponse) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString())
    }

    private fun cookie(
        value: String,
        maxAge: Duration,
    ): ResponseCookie =
        ResponseCookie
            .from(COOKIE_NAME, value)
            .httpOnly(true)
            .secure(cookieProperties.secure)
            .sameSite(SAME_SITE)
            .path(COOKIE_PATH)
            .maxAge(maxAge)
            .build()

    companion object {
        const val COOKIE_NAME = "signup_token"
        private const val COOKIE_PATH = "/api"
        private const val SAME_SITE = "Lax"
    }
}
