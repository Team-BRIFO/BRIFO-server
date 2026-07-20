package com.brifo.server.auth.controller

import com.brifo.server.auth.dto.KakaoLoginRequest
import com.brifo.server.auth.dto.KakaoLoginResponse
import com.brifo.server.auth.dto.LogoutRequest
import com.brifo.server.auth.dto.NaverLoginRequest
import com.brifo.server.auth.dto.NaverLoginResponse
import com.brifo.server.auth.service.KakaoLoginService
import com.brifo.server.auth.service.LogoutService
import com.brifo.server.auth.service.NaverLoginService
import com.brifo.server.global.code.SuccessCode
import com.brifo.server.global.common.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val kakaoLoginService: KakaoLoginService,
    private val naverLoginService: NaverLoginService,
    private val logoutService: LogoutService,
) {
    @PostMapping("/login/kakao")
    fun loginWithKakao(
        @Valid @RequestBody request: KakaoLoginRequest,
    ): ResponseEntity<ApiResponse<KakaoLoginResponse>> {
        val result = kakaoLoginService.login(request)
        val successCode =
            when (result) {
                is KakaoLoginResponse.Login -> SuccessCode.LOGIN
                is KakaoLoginResponse.SignupRequired -> SuccessCode.SIGNUP_REQUIRED
            }

        return ResponseEntity.ok(ApiResponse.success(successCode, result))
    }

    @PostMapping("/login/naver")
    fun loginWithNaver(
        @Valid @RequestBody request: NaverLoginRequest,
    ): ResponseEntity<ApiResponse<NaverLoginResponse>> {
        val result = naverLoginService.login(request)
        val successCode =
            when (result) {
                is NaverLoginResponse.Login -> SuccessCode.LOGIN
                is NaverLoginResponse.SignupRequired -> SuccessCode.SIGNUP_REQUIRED
            }

        return ResponseEntity.ok(ApiResponse.success(successCode, result))
    }

    @PostMapping("/logout")
    fun logout(
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) authorizationHeader: String?,
        @RequestBody(required = false) request: LogoutRequest?,
    ): ResponseEntity<ApiResponse<Nothing>> {
        logoutService.logout(authorizationHeader, request)
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK))
    }
}
