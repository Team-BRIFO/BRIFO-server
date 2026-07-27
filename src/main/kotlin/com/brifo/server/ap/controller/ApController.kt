package com.brifo.server.ap.controller

import com.brifo.server.ap.dto.request.CreateCreditLoanRequest
import com.brifo.server.ap.dto.request.GetApTransactionsRequest
import com.brifo.server.ap.dto.response.ApBalanceResponse
import com.brifo.server.ap.dto.response.CreateAttendanceRewardResponse
import com.brifo.server.ap.dto.response.GetApTransactionsResponse
import com.brifo.server.ap.service.ApService
import com.brifo.server.global.common.ApiResponse
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/ap")
class ApController(
    private val apService: ApService,
) {
    @GetMapping("/transactions")
    fun getApTransactions(
        @AuthenticationPrincipal userPublicId: UUID,
        @Valid @ModelAttribute request: GetApTransactionsRequest,
    ): ApiResponse<GetApTransactionsResponse> =
        ApiResponse.success(
            code = com.brifo.server.global.code.SuccessCode.OK,
            result = apService.getApTransactions(userPublicId, request),
        )

    @PostMapping("/attendance-rewards")
    fun createAttendanceReward(
        @AuthenticationPrincipal userPublicId: UUID,
    ): ApiResponse<CreateAttendanceRewardResponse> =
        ApiResponse.success(
            code = com.brifo.server.ap.code.ApSuccessCode.ATTENDANCE_REWARD_CLAIMED,
            result = apService.createAttendanceReward(userPublicId),
        )

    @PostMapping("/tutorial-rewards")
    fun createTutorialReward(
        @AuthenticationPrincipal userPublicId: UUID,
    ): ApiResponse<ApBalanceResponse> =
        ApiResponse.success(
            code = com.brifo.server.ap.code.ApSuccessCode.TUTORIAL_REWARD_CLAIMED,
            result = apService.createTutorialReward(userPublicId),
        )

    @PostMapping("/credit-loans")
    fun createCreditLoan(
        @AuthenticationPrincipal userPublicId: UUID,
        @Valid @RequestBody request: CreateCreditLoanRequest,
    ): ApiResponse<ApBalanceResponse> =
        ApiResponse.success(
            code = com.brifo.server.ap.code.ApSuccessCode.CREDIT_LOAN_CLAIMED,
            result = apService.createCreditLoan(userPublicId, request),
        )
}
