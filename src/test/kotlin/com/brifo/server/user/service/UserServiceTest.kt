package com.brifo.server.user.service

import com.brifo.server.agent.entity.Agent
import com.brifo.server.agent.entity.AgentType
import com.brifo.server.agent.repository.AgentRepository
import com.brifo.server.auth.dto.response.TokenInfo
import com.brifo.server.auth.service.JwtTokenProvider
import com.brifo.server.policy.repository.PolicyRepository
import com.brifo.server.policy.repository.UserPolicyRepository
import com.brifo.server.stock.entity.PendingUserStock
import com.brifo.server.stock.entity.Stock
import com.brifo.server.stock.entity.UserStock
import com.brifo.server.stock.exception.StockNotFoundException
import com.brifo.server.stock.repository.PendingUserStockRepository
import com.brifo.server.stock.repository.StockRepository
import com.brifo.server.stock.repository.UserStockRepository
import com.brifo.server.user.dto.request.UpdateOnboardingProfileRequest
import com.brifo.server.user.dto.request.UpdateUserProfileRequest
import com.brifo.server.user.entity.OAuthProvider
import com.brifo.server.user.entity.User
import com.brifo.server.user.exception.OnboardingProfileNotCompletedException
import com.brifo.server.user.exception.OnboardingStocksNotSelectedException
import com.brifo.server.user.exception.RequiredPoliciesNotAgreedException
import com.brifo.server.user.exception.UserNotFoundException
import com.brifo.server.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

class UserServiceTest {
    private val userRepository = mock(UserRepository::class.java)
    private val stockRepository = mock(StockRepository::class.java)
    private val userStockRepository = mock(UserStockRepository::class.java)
    private val pendingUserStockRepository = mock(PendingUserStockRepository::class.java)
    private val policyRepository = mock(PolicyRepository::class.java)
    private val userPolicyRepository = mock(UserPolicyRepository::class.java)
    private val agentRepository = mock(AgentRepository::class.java)
    private val validationService = mock(UserValidationService::class.java)
    private val jwtTokenProvider = mock(JwtTokenProvider::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-07-21T09:00:00Z"), ZoneId.of("Asia/Seoul"))
    private lateinit var userService: UserService

    @BeforeEach
    fun setUp() {
        userService =
            UserService(
                userRepository,
                stockRepository,
                userStockRepository,
                pendingUserStockRepository,
                policyRepository,
                userPolicyRepository,
                agentRepository,
                validationService,
                jwtTokenProvider,
                clock,
            )
    }

    @Test
    fun `온보딩 프로필을 공개 ID 사용자에게 반영한다`() {
        val userId = UUID.randomUUID()
        val user = user()
        val stockId = UUID.randomUUID()
        val selectedStock = stock(stockId)
        val request = UpdateOnboardingProfileRequest(" 입력 닉네임 ", " 입력 회사 ", listOf(stockId))
        `when`(userRepository.findForUpdateByPublicId(userId)).thenReturn(user)
        `when`(validationService.normalizeNickname(request.nickname)).thenReturn("닉네임")
        `when`(validationService.normalizeCompanyName(request.companyName)).thenReturn("회사")
        `when`(stockRepository.findAllByPublicIdInAndIsActiveTrue(request.stockIds)).thenReturn(listOf(selectedStock))

        userService.updateOnboardingProfile(userId, request)

        verify(validationService).requireOnboardingPending(user)
        verify(validationService).validateStockIds(request.stockIds)
        assertEquals("닉네임", user.nickname)
        assertEquals("회사", user.companyName)
        verify(userStockRepository).deleteAllByUser(user)
        @Suppress("UNCHECKED_CAST")
        val captor = ArgumentCaptor.forClass(List::class.java) as ArgumentCaptor<List<UserStock>>
        verify(userStockRepository).saveAll(captor.capture())
        assertEquals(listOf(stockId), captor.value.map { it.stock.publicId })
    }

    @Test
    fun `존재하지 않는 사용자의 온보딩 프로필은 수정하지 않는다`() {
        val userId = UUID.randomUUID()
        val request = UpdateOnboardingProfileRequest("닉네임", stockIds = listOf(UUID.randomUUID()))
        `when`(validationService.normalizeNickname(request.nickname)).thenReturn("닉네임")
        `when`(validationService.normalizeCompanyName(request.companyName)).thenReturn(null)
        `when`(userRepository.findForUpdateByPublicId(userId)).thenReturn(null)

        assertThrows(UserNotFoundException::class.java) {
            userService.updateOnboardingProfile(userId, request)
        }
    }

    @Test
    fun `프로필 관심 종목은 다음 날 자정 적용 예약으로 전부 교체한다`() {
        val userId = UUID.randomUUID()
        val user = user()
        val firstId = UUID.randomUUID()
        val secondId = UUID.randomUUID()
        val firstStock = stock(firstId)
        val secondStock = stock(secondId)
        val request = UpdateUserProfileRequest(" 입력 닉네임 ", " 입력 회사 ", listOf(firstId, secondId))
        `when`(validationService.normalizeNickname(request.nickname)).thenReturn("닉네임")
        `when`(validationService.normalizeCompanyName(request.companyName)).thenReturn("회사")
        `when`(userRepository.findForUpdateByPublicId(userId)).thenReturn(user)
        `when`(stockRepository.findAllByPublicIdInAndIsActiveTrue(request.stockIds))
            .thenReturn(listOf(firstStock, secondStock))

        userService.updateUserProfile(userId, request)

        assertEquals("닉네임", user.nickname)
        assertEquals("회사", user.companyName)
        verify(validationService).validateStockIds(request.stockIds)
        verify(pendingUserStockRepository).deleteAllByUser(user)
        @Suppress("UNCHECKED_CAST")
        val captor = ArgumentCaptor.forClass(List::class.java) as ArgumentCaptor<List<PendingUserStock>>
        verify(pendingUserStockRepository).saveAll(captor.capture())
        assertEquals(listOf(firstId, secondId), captor.value.map { it.stock.publicId })
        assertEquals(
            listOf(
                LocalDateTime.of(2026, 7, 22, 0, 0),
                LocalDateTime.of(2026, 7, 22, 0, 0),
            ),
            captor.value.map { it.effectiveAt },
        )
    }

    @Test
    fun `요청한 활성 종목을 모두 찾지 못하면 예약을 변경하지 않는다`() {
        val userId = UUID.randomUUID()
        val stockIds = listOf(UUID.randomUUID(), UUID.randomUUID())
        val request = UpdateUserProfileRequest("닉네임", "회사", stockIds)
        val existingStock = stock(stockIds[0])
        `when`(validationService.normalizeNickname(request.nickname)).thenReturn("닉네임")
        `when`(validationService.normalizeCompanyName(request.companyName)).thenReturn("회사")
        `when`(stockRepository.findAllByPublicIdInAndIsActiveTrue(stockIds)).thenReturn(listOf(existingStock))

        assertThrows(StockNotFoundException::class.java) {
            userService.updateUserProfile(userId, request)
        }

        verify(userRepository, never()).findForUpdateByPublicId(userId)
        verifyNoInteractions(pendingUserStockRepository)
    }

    @Test
    fun `회원 탈퇴도 사용자 행을 잠근 뒤 처리한다`() {
        val userId = UUID.randomUUID()
        val user = user()
        `when`(userRepository.findForUpdateByPublicId(userId)).thenReturn(user)

        userService.deleteUser(userId)

        verify(userRepository).delete(user)
    }

    @Test
    fun `온보딩 완료는 완료 시각과 기본 에이전트 명세를 반영한다`() {
        val userId = UUID.randomUUID()
        val user = user().also { it.updateOnboardingProfile("닉네임", null) }
        `when`(userRepository.findForUpdateByPublicId(userId)).thenReturn(user)
        `when`(policyRepository.countByIsRequiredTrueAndIsActiveTrue()).thenReturn(2)
        `when`(userPolicyRepository.countActiveRequiredAgreements(user)).thenReturn(2)
        `when`(userStockRepository.countByUser(user)).thenReturn(3)
        `when`(agentRepository.existsByUser(user)).thenReturn(false)
        `when`(jwtTokenProvider.issueLoginTokens(userId))
            .thenReturn(TokenInfo("access-token", "refresh-token", 3600, 1209600))

        val response = userService.completeOnboarding(userId)

        assertEquals(LocalDateTime.of(2026, 7, 21, 18, 0), user.onboardingCompletedAt)
        assertEquals("access-token", response.token.accessToken)
        @Suppress("UNCHECKED_CAST")
        val captor = ArgumentCaptor.forClass(List::class.java) as ArgumentCaptor<List<Agent>>
        verify(agentRepository).saveAll(captor.capture())
        assertEquals(
            listOf(
                AgentSpec(AgentType.ROOKIE, "Gemini 3.5 Flash", "루키", null, 10),
                AgentSpec(AgentType.PRO, "Claude Sonnet 4.6", "프로", null, 25),
                AgentSpec(AgentType.TANKER, "GPT-5.3", "탱커", null, 15),
            ),
            captor.value.map {
                AgentSpec(it.agentType, it.modelName, it.nickname, it.description, it.dailySalary)
            },
        )
    }

    @Test
    fun `온보딩 완료 조건을 순서대로 검증한다`() {
        val userId = UUID.randomUUID()
        val user = user()
        `when`(userRepository.findForUpdateByPublicId(userId)).thenReturn(user)
        `when`(policyRepository.countByIsRequiredTrueAndIsActiveTrue()).thenReturn(2)
        `when`(userPolicyRepository.countActiveRequiredAgreements(user)).thenReturn(1)

        assertThrows(RequiredPoliciesNotAgreedException::class.java) {
            userService.completeOnboarding(userId)
        }

        `when`(userPolicyRepository.countActiveRequiredAgreements(user)).thenReturn(2)
        assertThrows(OnboardingProfileNotCompletedException::class.java) {
            userService.completeOnboarding(userId)
        }

        user.updateOnboardingProfile("닉네임", null)
        `when`(userStockRepository.countByUser(user)).thenReturn(0)
        assertThrows(OnboardingStocksNotSelectedException::class.java) {
            userService.completeOnboarding(userId)
        }

        verifyNoInteractions(agentRepository)
    }

    private fun user(): User =
        User.create(OAuthProvider.KAKAO, "social-id", "user@example.com")

    private fun stock(publicId: UUID): Stock =
        mock(Stock::class.java).also {
            `when`(it.publicId).thenReturn(publicId)
        }

    private data class AgentSpec(
        val type: AgentType,
        val modelName: String,
        val nickname: String,
        val description: String?,
        val dailySalary: Int,
    )
}
