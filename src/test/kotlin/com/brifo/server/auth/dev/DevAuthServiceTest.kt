package com.brifo.server.auth.dev

import com.brifo.server.auth.dto.response.OAuthLoginResponse
import com.brifo.server.auth.service.JwtTokenProvider
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Answers
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class DevAuthServiceTest {
    private lateinit var oauthLoginService: OAuthLoginService

    @Mock
    private lateinit var policyRepository: PolicyRepository

    @Mock
    private lateinit var policyService: PolicyService

    @Mock
    private lateinit var stockRepository: StockRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var userService: UserService

    @Mock
    private lateinit var jwtTokenProvider: JwtTokenProvider

    private lateinit var initialApBalanceUpdater: DevInitialApBalanceUpdater
    private lateinit var service: DevAuthService
    private var updatedSocialId: String? = null
    private var updatedBalanceAp: Int? = null

    @BeforeEach
    fun setUp() {
        updatedSocialId = null
        updatedBalanceAp = null
        oauthLoginService =
            mock(OAuthLoginService::class.java) { invocation ->
                if (invocation.method.name == "login") {
                    OAuthLoginResponse.SignupRequired(signupToken = "signup-token")
                } else {
                    Answers.RETURNS_DEFAULTS.answer(invocation)
                }
            }
        initialApBalanceUpdater =
            mock(DevInitialApBalanceUpdater::class.java) { invocation ->
                if (invocation.method.name == "update") {
                    updatedSocialId = invocation.arguments[0] as String
                    updatedBalanceAp = invocation.arguments[1] as Int
                    null
                } else {
                    Answers.RETURNS_DEFAULTS.answer(invocation)
                }
            }
        service =
            DevAuthService(
                properties = DevAuthProperties(enabled = true, password = "password"),
                oauthLoginService = oauthLoginService,
                policyRepository = policyRepository,
                policyService = policyService,
                stockRepository = stockRepository,
                userRepository = userRepository,
                userService = userService,
                initialApBalanceUpdater = initialApBalanceUpdater,
                jwtTokenProvider = jwtTokenProvider,
            )
    }

    @Test
    fun `초기 AP를 지정하면 사용자 잔액과 최초 지급 거래를 함께 조정한다`() {
        val response = service.signUp(DevSignUpRequest("password", initialBalanceAp = 15))

        assertEquals("signup-token", response.signupToken)
        assertEquals(15, updatedBalanceAp)
        assertTrue(checkNotNull(updatedSocialId).startsWith("dev:"))
    }

    @Test
    fun `초기 AP를 생략하면 기존 최초 지급 잔액을 유지한다`() {
        val response = service.signUp(DevSignUpRequest("password"))

        assertEquals("signup-token", response.signupToken)
        verifyNoInteractions(initialApBalanceUpdater)
    }

    @Test
    fun `dev 온보딩은 비활성화된 데모 종목이 아닌 실제 활성 종목 코드를 조회한다`() {
        val userPublicId = UUID.randomUUID()
        val user = mock(User::class.java)
        `when`(user.socialId).thenReturn("dev:$userPublicId")
        `when`(userRepository.findByPublicId(userPublicId)).thenReturn(user)

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

        val result = service.completeOnboarding(userPublicId)

        assertEquals(response, result)
        activeCodes.forEach { code -> verify(stockRepository).findByCode(code) }
        listOf("BRIFO01", "BRIFO02", "BRIFO03").forEach { code ->
            verify(stockRepository, never()).findByCode(code)
        }
    }
}
