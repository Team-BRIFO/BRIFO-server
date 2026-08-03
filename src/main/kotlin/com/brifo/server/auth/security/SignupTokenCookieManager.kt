package com.brifo.server.auth.security

import com.brifo.server.auth.config.SignupTokenCookieProperties
import com.brifo.server.global.config.JwtProperties
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.util.Base64

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
        addCookie(response, cookie(COOKIE_NAME, signupToken, jwtProperties.signupTokenExpiration, httpOnly = true))
        addCookie(response, cookie(CSRF_COOKIE_NAME, newCsrfToken(), jwtProperties.signupTokenExpiration, httpOnly = false))
    }

    fun clear(response: HttpServletResponse) {
        addCookie(response, cookie(COOKIE_NAME, "", Duration.ZERO, httpOnly = true))
        addCookie(response, cookie(CSRF_COOKIE_NAME, "", Duration.ZERO, httpOnly = false))
    }

    private fun cookie(
        name: String,
        value: String,
        maxAge: Duration,
        httpOnly: Boolean,
    ): ResponseCookie =
        ResponseCookie
            .from(name, value)
            .httpOnly(httpOnly)
            .secure(cookieProperties.secure)
            .sameSite(SAME_SITE)
            .path(COOKIE_PATH)
            .maxAge(maxAge)
            .build()

    fun matchesCsrfToken(request: HttpServletRequest): Boolean {
        val cookieToken = resolveCookie(request, CSRF_COOKIE_NAME) ?: return false
        val headerToken = request.getHeader(CSRF_HEADER_NAME)?.takeIf { it.isNotBlank() } ?: return false
        return MessageDigest.isEqual(
            cookieToken.toByteArray(Charsets.UTF_8),
            headerToken.toByteArray(Charsets.UTF_8),
        )
    }

    private fun resolveCookie(
        request: HttpServletRequest,
        name: String,
    ): String? =
        request.cookies
            ?.firstOrNull { it.name == name }
            ?.value
            ?.takeIf { it.isNotBlank() }

    private fun newCsrfToken(): String =
        ByteArray(CSRF_TOKEN_BYTES)
            .also(secureRandom::nextBytes)
            .let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }

    private fun addCookie(
        response: HttpServletResponse,
        cookie: ResponseCookie,
    ) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
    }

    companion object {
        const val COOKIE_NAME = "signup_token"
        const val CSRF_COOKIE_NAME = "signup_csrf_token"
        const val CSRF_HEADER_NAME = "X-Signup-CSRF-Token"
        private const val COOKIE_PATH = "/api"
        private const val SAME_SITE = "Lax"
        private const val CSRF_TOKEN_BYTES = 32
        private val secureRandom = SecureRandom()
    }
}
