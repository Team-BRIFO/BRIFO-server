package com.brifo.server.auth.dev

import com.brifo.server.auth.dto.response.OAuthLoginResponse
import com.brifo.server.auth.service.OAuthLoginService
import com.brifo.server.policy.repository.PolicyRepository
import com.brifo.server.policy.service.PolicyService
import com.brifo.server.stock.repository.StockRepository
import com.brifo.server.user.repository.UserRepository
import com.brifo.server.user.service.UserService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Answers
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.junit.jupiter.MockitoExtension
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
}
