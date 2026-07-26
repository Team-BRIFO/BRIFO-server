package com.brifo.server.user.controller

import com.brifo.server.global.common.ApiResponse
import com.brifo.server.global.code.SuccessCode
import com.brifo.server.user.code.UserSuccessCode
import com.brifo.server.user.dto.request.UpdateOnboardingProfileRequest
import com.brifo.server.user.dto.request.UpdateUserProfileRequest
import com.brifo.server.user.dto.response.CompleteOnboardingResponse
import com.brifo.server.user.dto.response.GetMyPageResponse
import com.brifo.server.user.dto.response.GetUserHomeResponse
import com.brifo.server.user.dto.response.GetUserProfileResponse
import com.brifo.server.user.service.UserService
import com.brifo.server.user.service.UserQueryService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api")
class UserController(
    private val userService: UserService,
    private val userQueryService: UserQueryService,
) {
    @PatchMapping("/onboarding/profile")
    fun updateOnboardingProfile(
        @RequestParam userId: UUID,
        @Valid @RequestBody request: UpdateOnboardingProfileRequest,
    ): ApiResponse<Nothing> {
        userService.updateOnboardingProfile(userId, request)
        return ApiResponse.success(UserSuccessCode.ONBOARDING_PROFILE_UPDATED)
    }

    @PostMapping("/onboarding/complete")
    fun completeOnboarding(
        @RequestParam userId: UUID,
    ): ApiResponse<CompleteOnboardingResponse> {
        userService.completeOnboarding(userId)
        return ApiResponse.success(UserSuccessCode.ONBOARDING_COMPLETED)
    }

    @GetMapping("/users/me/profile")
    fun getUserProfile(
        @RequestParam userId: UUID,
    ): ApiResponse<GetUserProfileResponse> =
        ApiResponse.success(
            SuccessCode.OK,
            userQueryService.getUserProfile(userId),
        )

    @GetMapping("/users/me")
    fun getMyPage(
        @RequestParam userId: UUID,
    ): ApiResponse<GetMyPageResponse> =
        ApiResponse.success(
            SuccessCode.OK,
            userQueryService.getMyPage(userId),
        )

    @PatchMapping("/users/me/profile")
    fun updateUserProfile(
        @RequestParam userId: UUID,
        @Valid @RequestBody request: UpdateUserProfileRequest,
    ): ApiResponse<Nothing> {
        userService.updateUserProfile(userId, request)
        return ApiResponse.success(UserSuccessCode.PROFILE_UPDATED)
    }

    @DeleteMapping("/users/me")
    fun deleteUser(
        @RequestParam userId: UUID,
    ): ApiResponse<Nothing> {
        userService.deleteUser(userId)
        return ApiResponse.success(UserSuccessCode.USER_DELETED)
    }

    @GetMapping("/users/me/home")
    fun getUserHome(
        @RequestParam userId: UUID,
    ): ApiResponse<GetUserHomeResponse> =
        ApiResponse.success(
            SuccessCode.OK,
            userQueryService.getUserHome(userId),
        )
}
