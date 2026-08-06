package com.brifo.server.auth.security

import com.brifo.server.auth.code.AuthErrorCode
import com.brifo.server.auth.exception.AuthException
import com.brifo.server.auth.exception.InvalidJwtTokenException
import com.brifo.server.auth.service.JwtTokenProvider
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.web.filter.OncePerRequestFilter

class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val signupTokenCookieManager: SignupTokenCookieManager,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val tokenCandidate = resolveToken(request)

        if (tokenCandidate != null && SecurityContextHolder.getContext().authentication == null) {
            try {
                val claims = jwtTokenProvider.parseAuthenticationToken(tokenCandidate.value)
                if (claims.tokenType != tokenCandidate.expectedType) {
                    throw InvalidJwtTokenException()
                }
                val authentication =
                    UsernamePasswordAuthenticationToken(
                        claims.userPublicId,
                        null,
                        listOf(SimpleGrantedAuthority("${TOKEN_AUTHORITY_PREFIX}${claims.tokenType.name}")),
                    ).apply {
                        details = WebAuthenticationDetailsSource().buildDetails(request)
                    }
                SecurityContextHolder.getContext().authentication = authentication
            } catch (exception: AuthException) {
                SecurityContextHolder.clearContext()
                request.setAttribute(AUTH_ERROR_CODE_ATTRIBUTE, exception.errorCode)
                if (tokenCandidate.source == TokenSource.COOKIE) {
                    signupTokenCookieManager.clear(response)
                }
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun resolveToken(request: HttpServletRequest): TokenCandidate? {
        val authorization = request.getHeader(HttpHeaders.AUTHORIZATION)?.trim()
        if (authorization == null) {
            return signupTokenCookieManager.resolve(request)?.let {
                TokenCandidate(it, JwtTokenProvider.AuthenticationTokenType.SIGNUP, TokenSource.COOKIE)
            }
        }
        if (!authorization.startsWith(BEARER_PREFIX, ignoreCase = true)) {
            request.setAttribute(AUTH_ERROR_CODE_ATTRIBUTE, AuthErrorCode.INVALID_TOKEN)
            return null
        }

        return authorization
            .substring(BEARER_PREFIX.length)
            .trim()
            .takeIf { it.isNotEmpty() }
            ?.let { TokenCandidate(it, JwtTokenProvider.AuthenticationTokenType.ACCESS, TokenSource.HEADER) }
            ?: run {
                request.setAttribute(AUTH_ERROR_CODE_ATTRIBUTE, AuthErrorCode.INVALID_TOKEN)
                null
            }
    }

    private data class TokenCandidate(
        val value: String,
        val expectedType: JwtTokenProvider.AuthenticationTokenType,
        val source: TokenSource,
    )

    private enum class TokenSource {
        HEADER,
        COOKIE,
    }

    companion object {
        const val AUTH_ERROR_CODE_ATTRIBUTE = "auth.errorCode"
        const val ACCESS_AUTHORITY = "TOKEN_ACCESS"
        const val SIGNUP_AUTHORITY = "TOKEN_SIGNUP"
        private const val TOKEN_AUTHORITY_PREFIX = "TOKEN_"
        private const val BEARER_PREFIX = "Bearer "
    }
}
