package com.brifo.server.user.controller

import com.brifo.server.global.common.ApiResponse
import com.brifo.server.user.dto.request.UpdateOnboardingProfileRequest
import com.brifo.server.user.dto.request.UpdateUserProfileRequest
import com.brifo.server.user.dto.response.CompleteOnboardingResponse
import com.brifo.server.user.dto.response.GetMyPageResponse
import com.brifo.server.user.dto.response.GetUserHomeResponse
import com.brifo.server.user.dto.response.GetUserProfileResponse
import com.brifo.server.user.service.UserService
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class UserController(
    private val userService: UserService,
) {
    @PatchMapping("/onboarding/profile")
    fun updateOnboardingProfile(
        @RequestBody request: UpdateOnboardingProfileRequest,
    ): ApiResponse<Nothing> = TODO("온보딩 프로필 저장 서비스 구현 필요")

    @PostMapping("/onboarding/complete")
    fun completeOnboarding(): ApiResponse<CompleteOnboardingResponse> =
        TODO("온보딩 완료 서비스 구현 필요")

    @GetMapping("/users/me/profile")
    fun getUserProfile(): ApiResponse<GetUserProfileResponse> = TODO("사용자 프로필 조회 서비스 구현 필요")

    @GetMapping("/users/me")
    fun getMyPage(): ApiResponse<GetMyPageResponse> = TODO("마이페이지 조회 서비스 구현 필요")

    @PatchMapping("/users/me/profile")
    fun updateUserProfile(
        @RequestBody request: UpdateUserProfileRequest,
    ): ApiResponse<Nothing> = TODO("사용자 프로필 수정 서비스 구현 필요")

    @DeleteMapping("/users/me")
    fun deleteUser(): ApiResponse<Nothing> = TODO("회원 탈퇴 서비스 구현 필요")

    @GetMapping("/users/me/home")
    fun getUserHome(): ApiResponse<GetUserHomeResponse> = TODO("사용자 홈 조회 서비스 구현 필요")
}
