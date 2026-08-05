package com.brifo.server.auth.controller

import com.brifo.server.auth.dto.request.KakaoLoginRequest
import com.brifo.server.auth.dto.request.NaverLoginRequest
import com.brifo.server.auth.dto.request.RefreshTokenRequest
import com.brifo.server.auth.dto.response.OAuthLoginResponse
import com.brifo.server.auth.dto.response.ReissueResponse
import com.brifo.server.auth.security.SignupTokenCookieManager
import com.brifo.server.auth.service.KakaoLoginService
import com.brifo.server.auth.service.LogoutService
import com.brifo.server.auth.service.NaverLoginService
import com.brifo.server.auth.service.TokenReissueService
import com.brifo.server.global.code.SuccessCode
import com.brifo.server.global.common.ApiResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val kakaoLoginService: KakaoLoginService,
    private val naverLoginService: NaverLoginService,
    private val logoutService: LogoutService,
    private val tokenReissueService: TokenReissueService,
    private val signupTokenCookieManager: SignupTokenCookieManager,
) {
    @PostMapping("/login/kakao")
    fun loginWithKakao(
        @Valid @RequestBody request: KakaoLoginRequest,
        response: HttpServletResponse,
    ): ApiResponse<OAuthLoginResponse> {
        val result = kakaoLoginService.login(request)
        updateSignupTokenCookie(result, response)
        return ApiResponse.success(SuccessCode.OK, result)
    }

    @PostMapping("/login/naver")
    fun loginWithNaver(
        @Valid @RequestBody request: NaverLoginRequest,
        response: HttpServletResponse,
    ): ApiResponse<OAuthLoginResponse> {
        val result = naverLoginService.login(request)
        updateSignupTokenCookie(result, response)
        return ApiResponse.success(SuccessCode.OK, result)
    }

    @PostMapping("/logout")
    fun logout(
        @AuthenticationPrincipal userPublicId: UUID,
        @RequestBody(required = false) request: RefreshTokenRequest?,
    ): ApiResponse<Nothing> {
        logoutService.logout(userPublicId, request)
        return ApiResponse.success(SuccessCode.OK)
    }

    @PostMapping("/reissue")
    fun reissue(
        @RequestBody(required = false) request: RefreshTokenRequest?,
    ): ApiResponse<ReissueResponse> {
        val result = tokenReissueService.reissue(request)
        return ApiResponse.success(SuccessCode.OK, result)
    }

    @GetMapping("/signup/csrf")
    fun refreshSignupCsrfToken(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): ApiResponse<Nothing> {
        signupTokenCookieManager.refreshCsrfToken(request, response)
        return ApiResponse.success(SuccessCode.OK)
    }

    private fun updateSignupTokenCookie(
        result: OAuthLoginResponse,
        response: HttpServletResponse,
    ) {
        when (result) {
            is OAuthLoginResponse.SignupRequired -> signupTokenCookieManager.set(response, result.signupToken)
            is OAuthLoginResponse.Login -> signupTokenCookieManager.clear(response)
        }
    }
}
