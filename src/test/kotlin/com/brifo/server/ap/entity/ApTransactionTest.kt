package com.brifo.server.ap.entity

import com.brifo.server.user.entity.OAuthProvider
import com.brifo.server.user.entity.User
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ApTransactionTest {
    @Test
    fun `급여와 환불 거래는 반대 부호로 생성된다`() {
        val user = User.create(
            provider = OAuthProvider.KAKAO,
            socialId = "social-id",
            email = "user@example.com",
        )

        val salary = ApTransaction.salary(user, briefingId = 1L, salaryCost = 10)
        val refund = ApTransaction.salaryRefund(user, briefingId = 1L, refundAmount = 10)

        assertEquals(-10, salary.amount)
        assertEquals(ApTransactionReason.SALARY, salary.reason)
        assertEquals(10, refund.amount)
        assertEquals(ApTransactionReason.SALARY_REFUND, refund.reason)
    }

    @Test
    fun `사용 가능한 AP보다 많이 차감할 수 없다`() {
        val user = User.create(
            provider = OAuthProvider.KAKAO,
            socialId = "social-id",
            email = "user@example.com",
        )
        user.changeAp(10)

        assertFailsWith<IllegalArgumentException> { user.changeAp(-11) }
        assertEquals(10, user.balanceAp)
    }
}
