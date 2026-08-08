package com.brifo.server.auth.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.web.filter.OncePerRequestFilter

class SignupCsrfFilter(
    private val signupTokenCookieManager: SignupTokenCookieManager,
    private val accessDeniedHandler: AccessDeniedHandler,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        if (requiresProtection(request) && !signupTokenCookieManager.matchesCsrfToken(request)) {
            accessDeniedHandler.handle(request, response, AccessDeniedException("Invalid signup CSRF token"))
            return
        }

        filterChain.doFilter(request, response)
    }

    private fun requiresProtection(request: HttpServletRequest): Boolean {
        if (request.method in SAFE_METHODS || request.requestURI == request.contextPath + DEV_SIGNUP_PATH) {
            return false
        }

        return SecurityContextHolder
            .getContext()
            .authentication
            ?.authorities
            ?.any { it.authority == JwtAuthenticationFilter.SIGNUP_AUTHORITY } == true
    }

    companion object {
        private const val DEV_SIGNUP_PATH = "/api/dev/signup"
        private val SAFE_METHODS = setOf("GET", "HEAD", "OPTIONS", "TRACE")
    }
}
