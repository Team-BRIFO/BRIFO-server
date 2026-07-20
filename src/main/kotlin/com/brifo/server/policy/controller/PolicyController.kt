package com.brifo.server.policy.controller

import com.brifo.server.global.common.ApiResponse
import com.brifo.server.policy.dto.request.AgreePoliciesRequest
import com.brifo.server.policy.dto.response.GetPendingPoliciesResponse
import com.brifo.server.policy.dto.response.GetPoliciesResponse
import com.brifo.server.policy.dto.response.GetPolicyDetailResponse
import com.brifo.server.policy.service.PolicyService
import jakarta.validation.Valid
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
    fun getPolicies(): ApiResponse<GetPoliciesResponse> = TODO("약관 목록 조회 서비스 구현 필요")

    @GetMapping("/policies/{policyId}")
    fun getPolicyDetail(
        @PathVariable policyId: UUID,
    ): ApiResponse<GetPolicyDetailResponse> = TODO("약관 상세 조회 서비스 구현 필요")

    @PostMapping("/users/me/policies")
    fun agreePolicies(
        @Valid @RequestBody request: AgreePoliciesRequest,
    ): ApiResponse<Nothing> = TODO("약관 동의 서비스 구현 필요")

    @GetMapping("/users/me/policies/pending")
    fun getPendingPolicies(): ApiResponse<GetPendingPoliciesResponse> =
        TODO("재동의 필요 약관 조회 서비스 구현 필요")

    @DeleteMapping("/users/me/policies/{policyId}")
    fun revokePolicy(
        @PathVariable policyId: UUID,
    ): ApiResponse<Nothing> = TODO("선택 약관 철회 서비스 구현 필요")
}
