package com.brifo.server.auth.dev

import com.brifo.server.global.code.SuccessCode
import com.brifo.server.global.common.ApiResponse
import com.brifo.server.user.dto.response.CompleteOnboardingResponse
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@Profile("dev")
@ConditionalOnProperty(prefix = "app.dev-auth", name = ["enabled"], havingValue = "true")
@RequestMapping("/api/dev/")
class DevAuthController(
    private val devAuthService: DevAuthService,
) {
    @PostMapping("/signup")
    fun signUp(
        @RequestBody request: DevSignUpRequest,
    ): ApiResponse<DevSignUpResponse> =
        ApiResponse.success(SuccessCode.OK, devAuthService.signUp(request))

    @PostMapping("/onboarding/complete")
    fun completeOnboarding(
        @AuthenticationPrincipal userPublicId: UUID,
    ): ApiResponse<CompleteOnboardingResponse> =
        ApiResponse.success(
            SuccessCode.OK,
            devAuthService.completeOnboarding(userPublicId),
        )
}
