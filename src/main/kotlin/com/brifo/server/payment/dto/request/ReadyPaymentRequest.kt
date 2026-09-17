package com.brifo.server.payment.dto.request

import jakarta.validation.constraints.Positive

data class ReadyPaymentRequest(
    @field:Positive
    val amount: Int,
)
