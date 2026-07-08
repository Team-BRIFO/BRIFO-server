package com.brifo.server.auth.controller

import com.brifo.server.auth.dto.KakaoLoginRequest
import com.brifo.server.auth.dto.KakaoLoginResponse
import com.brifo.server.auth.service.KakaoLoginService
import com.brifo.server.global.code.SuccessCode
import com.brifo.server.global.common.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val kakaoLoginService: KakaoLoginService,
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
}
