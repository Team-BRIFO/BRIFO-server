package com.brifo.server.ap.controller

import com.brifo.server.ap.dto.request.CreateCreditLoanRequest
import com.brifo.server.ap.dto.request.GetApTransactionsRequest
import com.brifo.server.ap.dto.response.ApBalanceResponse
import com.brifo.server.ap.dto.response.CreateAttendanceRewardResponse
import com.brifo.server.ap.dto.response.GetApTransactionsResponse
import com.brifo.server.ap.service.ApService
import com.brifo.server.global.common.ApiResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/ap")
class ApController(
    private val apService: ApService,
) {
    @GetMapping("/transactions")
    fun getApTransactions(
        @Valid @ModelAttribute request: GetApTransactionsRequest,
    ): ApiResponse<GetApTransactionsResponse> = TODO("AP 거래 목록 조회 서비스 구현 필요")

    @PostMapping("/attendance-rewards")
    fun createAttendanceReward(): ApiResponse<CreateAttendanceRewardResponse> =
        TODO("출석 보상 지급 서비스 구현 필요")

    @PostMapping("/tutorial-rewards")
    fun createTutorialReward(): ApiResponse<ApBalanceResponse> =
        TODO("튜토리얼 보상 지급 서비스 구현 필요")

    @PostMapping("/credit-loans")
    fun createCreditLoan(
        @Valid @RequestBody request: CreateCreditLoanRequest,
    ): ApiResponse<ApBalanceResponse> = TODO("신용대출 AP 지급 서비스 구현 필요")
}
