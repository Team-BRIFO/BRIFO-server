package com.brifo.server.payment.repository

import com.brifo.server.payment.entity.Payment
import org.springframework.data.jpa.repository.JpaRepository

interface PaymentRepository : JpaRepository<Payment, Long> {
    fun findByOrderId(orderId: String): Payment?
}
