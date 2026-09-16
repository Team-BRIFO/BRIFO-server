package com.brifo.server.auth.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpMethod
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
        if (
            request.method in SAFE_METHODS ||
            (
                request.method == HttpMethod.POST.name() &&
                    CSRF_EXEMPT_POST_PATHS.any { request.requestURI == request.contextPath + it }
            )
        ) {
            return false
        }

        return SecurityContextHolder
            .getContext()
            .authentication
            ?.authorities
            ?.any { it.authority == JwtAuthenticationFilter.SIGNUP_AUTHORITY } == true
    }

    companion object {
        /**
         * CSRF 검사를 면제하는 POST 경로.
         * - /api/dev/signup: 개발용 가입 진입점
         * - /api/auth/signup/cancel: 가입 세션을 버리기만 하는 요청. 로그인이 끊긴 뒤라
         *   클라이언트에 CSRF 토큰이 남아있지 않은 상태에서 호출되므로, 토큰을 요구하면
         *   정작 정리가 필요한 순간에 거절된다.
         */
        private val CSRF_EXEMPT_POST_PATHS = setOf("/api/dev/signup", "/api/auth/signup/cancel")
        private val SAFE_METHODS = setOf("GET", "HEAD", "OPTIONS", "TRACE")
    }
}
