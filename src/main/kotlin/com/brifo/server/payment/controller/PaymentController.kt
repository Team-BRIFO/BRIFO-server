package com.brifo.server.payment.controller

import com.brifo.server.ap.dto.response.ApBalanceResponse
import com.brifo.server.global.common.ApiResponse
import com.brifo.server.payment.code.PaymentSuccessCode
import com.brifo.server.payment.dto.request.ConfirmPaymentRequest
import com.brifo.server.payment.dto.request.ReadyPaymentRequest
import com.brifo.server.payment.dto.response.ReadyPaymentResponse
import com.brifo.server.payment.service.PaymentService
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/payments")
class PaymentController(
    private val paymentService: PaymentService,
) {
    @PostMapping("/ready")
    fun ready(
        @AuthenticationPrincipal userPublicId: UUID,
        @Valid @RequestBody request: ReadyPaymentRequest,
    ): ApiResponse<ReadyPaymentResponse> =
        ApiResponse.success(
            code = PaymentSuccessCode.PAYMENT_READY,
            result = paymentService.ready(userPublicId, request),
        )

    @PostMapping("/confirm")
    fun confirm(
        @AuthenticationPrincipal userPublicId: UUID,
        @Valid @RequestBody request: ConfirmPaymentRequest,
    ): ApiResponse<ApBalanceResponse> =
        ApiResponse.success(
            code = PaymentSuccessCode.PAYMENT_CONFIRMED,
            result = paymentService.confirm(userPublicId, request),
        )
}
