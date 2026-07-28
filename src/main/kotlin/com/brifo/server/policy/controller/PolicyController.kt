package com.brifo.server.policy.controller

import com.brifo.server.global.common.ApiResponse
import com.brifo.server.global.code.SuccessCode
import com.brifo.server.policy.dto.request.AgreePoliciesRequest
import com.brifo.server.policy.dto.response.GetPendingPoliciesResponse
import com.brifo.server.policy.dto.response.GetPoliciesResponse
import com.brifo.server.policy.dto.response.GetPolicyDetailResponse
import com.brifo.server.policy.service.PolicyService
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api")
class PolicyController(
    private val policyService: PolicyService,
) {
    @GetMapping("/policies")
    fun getPolicies(
        @AuthenticationPrincipal userPublicId: UUID,
    ): ApiResponse<GetPoliciesResponse> =
        ApiResponse.success(
            code = SuccessCode.OK,
            result = policyService.getPolicies(userPublicId),
        )

    @GetMapping("/policies/{policyId}")
    fun getPolicyDetail(
        @PathVariable policyId: UUID,
    ): ApiResponse<GetPolicyDetailResponse> =
        ApiResponse.success(
            code = SuccessCode.OK,
            result = policyService.getPolicyDetail(policyId),
        )

    @PostMapping("/users/me/policies")
    fun agreePolicies(
        @AuthenticationPrincipal userPublicId: UUID,
        @Valid @RequestBody request: AgreePoliciesRequest,
    ): ApiResponse<Nothing> {
        policyService.agreePolicies(userPublicId, request.policyIds)
        return ApiResponse.success(SuccessCode.OK)
    }

    @GetMapping("/users/me/policies/pending")
    fun getPendingPolicies(
        @AuthenticationPrincipal userPublicId: UUID,
    ): ApiResponse<GetPendingPoliciesResponse> =
        ApiResponse.success(
            code = SuccessCode.OK,
            result = policyService.getPendingPolicies(userPublicId),
        )

    @DeleteMapping("/users/me/policies/{policyId}")
    fun revokePolicy(
        @AuthenticationPrincipal userPublicId: UUID,
        @PathVariable policyId: UUID,
    ): ApiResponse<Nothing> {
        policyService.revokePolicy(userPublicId, policyId)
        return ApiResponse.success(SuccessCode.OK)
    }
}
