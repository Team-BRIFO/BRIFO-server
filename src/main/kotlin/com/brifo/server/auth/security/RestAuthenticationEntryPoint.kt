package com.brifo.server.auth.security

import com.brifo.server.auth.code.AuthErrorCode
import com.brifo.server.global.code.BaseCode
import com.brifo.server.global.common.ApiResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import tools.jackson.databind.ObjectMapper

class RestAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper,
) : AuthenticationEntryPoint {
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        val errorCode =
            request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_CODE_ATTRIBUTE) as? BaseCode
                ?: AuthErrorCode.UNAUTHORIZED

        response.status = errorCode.status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        objectMapper.writeValue(response.outputStream, ApiResponse.error(errorCode))
    }
}
