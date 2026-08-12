package com.brifo.server.auth.dev

import com.brifo.server.auth.service.OAuthLoginService
import com.brifo.server.policy.entity.Policy
import com.brifo.server.policy.repository.PolicyRepository
import com.brifo.server.policy.service.PolicyService
import com.brifo.server.stock.entity.Stock
import com.brifo.server.stock.repository.StockRepository
import com.brifo.server.user.dto.response.CompleteOnboardingResponse
import com.brifo.server.user.entity.User
import com.brifo.server.user.repository.UserRepository
import com.brifo.server.user.service.UserService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID
import kotlin.test.assertEquals

class DevAuthServiceTest {
    private val properties = DevAuthProperties(enabled = true, password = "dev-password")
    private val oauthLoginService = mock(OAuthLoginService::class.java)
    private val policyRepository = mock(PolicyRepository::class.java)
    private val policyService = mock(PolicyService::class.java)
    private val stockRepository = mock(StockRepository::class.java)
    private val userRepository = mock(UserRepository::class.java)
    private val userService = mock(UserService::class.java)

    private val devAuthService =
        DevAuthService(
            properties,
            oauthLoginService,
            policyRepository,
            policyService,
            stockRepository,
            userRepository,
            userService,
        )

    @Test
    fun `dev 온보딩은 비활성화된 데모 종목이 아닌 실제 활성 종목 코드를 조회한다`() {
        val userPublicId = UUID.randomUUID()
        val user = mock(User::class.java)
        `when`(user.socialId).thenReturn("dev:$userPublicId")
        `when`(userRepository.findByPublicId(userPublicId)).thenReturn(user)

        // R__stocks.sql / V94 마이그레이션 이후 실제로 존재하는 활성 종목 코드
        val activeCodes = listOf("005930", "000660", "035420")
        activeCodes.forEach { code ->
            val stock = mock(Stock::class.java)
            `when`(stock.publicId).thenReturn(UUID.randomUUID())
            `when`(stockRepository.findByCode(code)).thenReturn(stock)
        }

        val policy = mock(Policy::class.java)
        `when`(policy.isActive).thenReturn(true)
        `when`(policy.publicId).thenReturn(UUID.randomUUID())
        `when`(policyRepository.findAll()).thenReturn(listOf(policy))

        val response = mock(CompleteOnboardingResponse::class.java)
        `when`(userService.completeOnboarding(userPublicId)).thenReturn(response)

        val result = devAuthService.completeOnboarding(userPublicId)

        assertEquals(response, result)
        activeCodes.forEach { code -> verify(stockRepository).findByCode(code) }
        // V94 마이그레이션으로 비활성화된 데모 종목은 더 이상 조회하지 않는다.
        listOf("BRIFO01", "BRIFO02", "BRIFO03").forEach { code ->
            verify(stockRepository, never()).findByCode(code)
        }
    }
}
