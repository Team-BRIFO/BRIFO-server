package com.brifo.server.auth.security

import com.brifo.server.auth.code.AuthErrorCode
import com.brifo.server.auth.exception.AuthException
import com.brifo.server.auth.service.JwtTokenProvider
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.web.filter.OncePerRequestFilter

class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val accessToken = resolveAccessToken(request)

        if (accessToken != null && SecurityContextHolder.getContext().authentication == null) {
            try {
                val claims = jwtTokenProvider.parseAccessToken(accessToken)
                val authentication =
                    UsernamePasswordAuthenticationToken(
                        claims.userPublicId,
                        null,
                        emptyList(),
                    ).apply {
                        details = WebAuthenticationDetailsSource().buildDetails(request)
                    }
                SecurityContextHolder.getContext().authentication = authentication
            } catch (exception: AuthException) {
                SecurityContextHolder.clearContext()
                request.setAttribute(AUTH_ERROR_CODE_ATTRIBUTE, exception.errorCode)
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun resolveAccessToken(request: HttpServletRequest): String? {
        val authorization = request.getHeader(HttpHeaders.AUTHORIZATION)?.trim() ?: return null
        if (!authorization.startsWith(BEARER_PREFIX, ignoreCase = true)) {
            request.setAttribute(AUTH_ERROR_CODE_ATTRIBUTE, AuthErrorCode.INVALID_TOKEN)
            return null
        }

        return authorization
            .substring(BEARER_PREFIX.length)
            .trim()
            .takeIf { it.isNotEmpty() }
            ?: run {
                request.setAttribute(AUTH_ERROR_CODE_ATTRIBUTE, AuthErrorCode.INVALID_TOKEN)
                null
            }
    }

    companion object {
        const val AUTH_ERROR_CODE_ATTRIBUTE = "auth.errorCode"
        private const val BEARER_PREFIX = "Bearer "
    }
}
