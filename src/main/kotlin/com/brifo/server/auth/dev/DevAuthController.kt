package com.brifo.server.auth.dev

import com.brifo.server.auth.security.SignupTokenCookieManager
import com.brifo.server.global.code.SuccessCode
import com.brifo.server.global.common.ApiResponse
import com.brifo.server.user.dto.response.CompleteOnboardingResponse
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
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
@RequestMapping("/api/dev")
class DevAuthController(
    private val devAuthService: DevAuthService,
    private val signupTokenCookieManager: SignupTokenCookieManager,
) {
    @PostMapping("/signup")
    fun signUp(
        @Valid @RequestBody request: DevSignUpRequest,
        response: HttpServletResponse,
    ): ApiResponse<DevSignUpResponse> {
        val result = devAuthService.signUp(request)
        val csrfToken = signupTokenCookieManager.set(response, result.signupToken)
        return ApiResponse.success(SuccessCode.OK, result.copy(csrfToken = csrfToken))
    }

    @PostMapping("/onboarding/complete")
    fun completeOnboarding(
        @AuthenticationPrincipal userPublicId: UUID,
        response: HttpServletResponse,
    ): ApiResponse<CompleteOnboardingResponse> {
        val result = devAuthService.completeOnboarding(userPublicId)
        signupTokenCookieManager.clear(response)
        return ApiResponse.success(SuccessCode.OK, result)
    }
}
