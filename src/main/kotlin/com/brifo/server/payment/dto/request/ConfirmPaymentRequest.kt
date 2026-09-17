package com.brifo.server.payment.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive

data class ConfirmPaymentRequest(
    @field:NotBlank
    val paymentKey: String,
    @field:NotBlank
    val orderId: String,
    @field:Positive
    val amount: Int,
)
