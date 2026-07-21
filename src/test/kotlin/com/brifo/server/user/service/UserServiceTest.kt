package com.brifo.server.user.service

import com.brifo.server.agent.entity.Agent
import com.brifo.server.agent.entity.AgentType
import com.brifo.server.agent.repository.AgentRepository
import com.brifo.server.news.entity.NewsSource
import com.brifo.server.policy.repository.PolicyRepository
import com.brifo.server.policy.repository.UserPolicyRepository
import com.brifo.server.stock.entity.Stock
import com.brifo.server.stock.entity.UserStock
import com.brifo.server.stock.exception.DuplicatedStockSelectionException
import com.brifo.server.stock.exception.StockNotFoundException
import com.brifo.server.stock.exception.StockSelectionMaximumExceededException
import com.brifo.server.stock.exception.StockSelectionMinimumNotMetException
import com.brifo.server.stock.repository.StockRepository
import com.brifo.server.stock.repository.UserStockRepository
import com.brifo.server.user.dto.request.UpdateOnboardingProfileRequest
import com.brifo.server.user.dto.request.UpdateUserProfileRequest
import com.brifo.server.user.entity.OAuthProvider
import com.brifo.server.user.entity.User
import com.brifo.server.user.exception.InvalidCompanyNameException
import com.brifo.server.user.exception.InvalidNicknameException
import com.brifo.server.user.exception.OnboardingAlreadyCompletedException
import com.brifo.server.user.exception.OnboardingProfileNotCompletedException
import com.brifo.server.user.exception.OnboardingStocksNotSelectedException
import com.brifo.server.user.exception.RequiredPoliciesNotAgreedException
import com.brifo.server.user.exception.UserNotFoundException
import com.brifo.server.user.repository.UserHomeAgent
import com.brifo.server.user.repository.UserHomeData
import com.brifo.server.user.repository.UserHomeNewsCard
import com.brifo.server.user.repository.UserHomeQueryRepository
import com.brifo.server.user.repository.UserMyPageQueryRepository
import com.brifo.server.user.repository.UserMyPageStats
import com.brifo.server.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

class UserServiceTest {
    private lateinit var userRepository: UserRepository
    private lateinit var userMyPageQueryRepository: UserMyPageQueryRepository
    private lateinit var userHomeQueryRepository: UserHomeQueryRepository
    private lateinit var stockRepository: StockRepository
    private lateinit var userStockRepository: UserStockRepository
    private lateinit var policyRepository: PolicyRepository
    private lateinit var userPolicyRepository: UserPolicyRepository
    private lateinit var agentRepository: AgentRepository
    private lateinit var userService: UserService

    private val clock = Clock.fixed(Instant.parse("2026-07-21T09:00:00Z"), ZoneId.of("Asia/Seoul"))

    @BeforeEach
    fun setUp() {
        userRepository = mock(UserRepository::class.java)
        userMyPageQueryRepository = mock(UserMyPageQueryRepository::class.java)
        userHomeQueryRepository = mock(UserHomeQueryRepository::class.java)
        stockRepository = mock(StockRepository::class.java)
        userStockRepository = mock(UserStockRepository::class.java)
        policyRepository = mock(PolicyRepository::class.java)
        userPolicyRepository = mock(UserPolicyRepository::class.java)
        agentRepository = mock(AgentRepository::class.java)
        userService =
            UserService(
                userRepository,
                userMyPageQueryRepository,
                userHomeQueryRepository,
                stockRepository,
                userStockRepository,
                policyRepository,
                userPolicyRepository,
                agentRepository,
                clock,
            )
    }

    @Test
    fun `온보딩 프로필은 토큰 사용자의 소셜 식별자로 조회하고 trim 후 저장한다`() {
        val user = user()
        `when`(userRepository.findByProviderAndSocialId(OAuthProvider.KAKAO, "social-id")).thenReturn(user)

        userService.updateOnboardingProfile(
            provider = OAuthProvider.KAKAO,
            socialId = "social-id",
            request =
                UpdateOnboardingProfileRequest(
                    nickname = "  brifo  ",
                    companyName = "  내 투자회사  ",
                ),
        )

        verify(userRepository).findByProviderAndSocialId(OAuthProvider.KAKAO, "social-id")
        assertEquals("brifo", user.nickname)
        assertEquals("내 투자회사", user.companyName)
    }

    @Test
    fun `인증 연동 전에는 공개 ID로 온보딩 사용자를 조회해 프로필을 저장한다`() {
        val userPublicId = UUID.randomUUID()
        val user = user()
        `when`(userRepository.findByPublicId(userPublicId)).thenReturn(user)

        userService.updateOnboardingProfile(
            userPublicId = userPublicId,
            request =
                UpdateOnboardingProfileRequest(
                    nickname = "  brifo  ",
                    companyName = "  내 투자회사  ",
                ),
        )

        verify(userRepository).findByPublicId(userPublicId)
        assertEquals("brifo", user.nickname)
        assertEquals("내 투자회사", user.companyName)
    }

    @Test
    fun `회사명이 없으면 기존 기본 회사명을 유지한다`() {
        val user = user()
        `when`(userRepository.findByProviderAndSocialId(OAuthProvider.KAKAO, "social-id")).thenReturn(user)

        userService.updateOnboardingProfile(
            provider = OAuthProvider.KAKAO,
            socialId = "social-id",
            request = UpdateOnboardingProfileRequest(nickname = "brifo"),
        )

        assertEquals("brifo", user.nickname)
        assertEquals("내 투자회사", user.companyName)
    }

    @Test
    fun `닉네임은 trim 후 1자에서 50자까지 허용한다`() {
        listOf("   ", "a".repeat(51)).forEach { nickname ->
            assertThrows(InvalidNicknameException::class.java) {
                userService.updateOnboardingProfile(
                    provider = OAuthProvider.KAKAO,
                    socialId = "social-id",
                    request = UpdateOnboardingProfileRequest(nickname = nickname),
                )
            }
        }

        verifyNoInteractions(userRepository)
    }

    @Test
    fun `입력된 회사명은 trim 후 1자에서 100자까지 허용한다`() {
        listOf("   ", "a".repeat(101)).forEach { companyName ->
            assertThrows(InvalidCompanyNameException::class.java) {
                userService.updateOnboardingProfile(
                    provider = OAuthProvider.KAKAO,
                    socialId = "social-id",
                    request =
                        UpdateOnboardingProfileRequest(
                            nickname = "brifo",
                            companyName = companyName,
                        ),
                )
            }
        }

        verifyNoInteractions(userRepository)
    }

    @Test
    fun `존재하지 않는 소셜 사용자는 USER_404 예외를 던진다`() {
        `when`(userRepository.findByProviderAndSocialId(OAuthProvider.NAVER, "unknown-id")).thenReturn(null)

        assertThrows(UserNotFoundException::class.java) {
            userService.updateOnboardingProfile(
                provider = OAuthProvider.NAVER,
                socialId = "unknown-id",
                request = UpdateOnboardingProfileRequest(nickname = "brifo"),
            )
        }
    }

    @Test
    fun `온보딩을 완료한 사용자는 프로필을 다시 저장할 수 없다`() {
        val user = mock(User::class.java)
        `when`(user.onboardingCompletedAt).thenReturn(LocalDateTime.of(2026, 7, 21, 16, 0))
        `when`(userRepository.findByProviderAndSocialId(OAuthProvider.KAKAO, "social-id")).thenReturn(user)

        assertThrows(OnboardingAlreadyCompletedException::class.java) {
            userService.updateOnboardingProfile(
                provider = OAuthProvider.KAKAO,
                socialId = "social-id",
                request = UpdateOnboardingProfileRequest(nickname = "brifo"),
            )
        }
    }

    @Test
    fun `마이페이지는 사용자 정보와 집계 통계를 반환한다`() {
        val userPublicId = UUID.randomUUID()
        val user = myPageUser()
        val weekStart = LocalDateTime.of(2026, 7, 20, 0, 0)
        val now = LocalDateTime.of(2026, 7, 21, 18, 0)
        val stats =
            UserMyPageStats(
                thisWeekEarnedAp = 450,
                correctDecisionCount = 2,
                settledDecisionCount = 3,
                totalDecisionCount = 48,
                latestConsecutiveDays = 5,
                latestAttendanceAt = LocalDateTime.of(2026, 7, 20, 9, 0),
                learnedTermCount = 24,
            )
        `when`(userRepository.findByPublicId(userPublicId)).thenReturn(user)
        `when`(userMyPageQueryRepository.getMyPageStats(7L, weekStart, now)).thenReturn(stats)

        val response = userService.getMyPage(userPublicId)

        assertEquals("brifo", response.nickname)
        assertEquals("내 투자회사", response.companyName)
        assertEquals(1250, response.balanceAp)
        assertEquals(450, response.thisWeekEarnedAp)
        assertEquals(67, response.decisionAccuracyRate)
        assertEquals(48, response.totalDecision)
        assertEquals(5, response.consecutiveDays)
        assertEquals(24, response.learnedTermCount)
        verify(userMyPageQueryRepository).getMyPageStats(7L, weekStart, now)
    }

    @Test
    fun `정산된 결정과 유효한 최근 출석이 없으면 적중률과 연속 출석은 0이다`() {
        val userPublicId = UUID.randomUUID()
        val user = myPageUser()
        val stats =
            UserMyPageStats(
                thisWeekEarnedAp = 0,
                correctDecisionCount = 0,
                settledDecisionCount = 0,
                totalDecisionCount = 2,
                latestConsecutiveDays = 7,
                latestAttendanceAt = LocalDateTime.of(2026, 7, 19, 9, 0),
                learnedTermCount = 0,
            )
        `when`(userRepository.findByPublicId(userPublicId)).thenReturn(user)
        `when`(
            userMyPageQueryRepository.getMyPageStats(
                7L,
                LocalDateTime.of(2026, 7, 20, 0, 0),
                LocalDateTime.of(2026, 7, 21, 18, 0),
            ),
        ).thenReturn(stats)

        val response = userService.getMyPage(userPublicId)

        assertEquals(0, response.decisionAccuracyRate)
        assertEquals(0, response.consecutiveDays)
    }

    @Test
    fun `마이페이지 사용자가 없으면 USER_404 예외를 던진다`() {
        val userPublicId = UUID.randomUUID()
        `when`(userRepository.findByPublicId(userPublicId)).thenReturn(null)

        assertThrows(UserNotFoundException::class.java) {
            userService.getMyPage(userPublicId)
        }

        verifyNoInteractions(userMyPageQueryRepository)
    }

    @Test
    fun `홈 화면은 에이전트를 고정 순서로 정렬하고 오늘의 정보를 반환한다`() {
        val userPublicId = UUID.randomUUID()
        val user = myPageUser()
        val batchTime = LocalDateTime.of(2026, 7, 21, 8, 0)
        val cardId = UUID.randomUUID()
        val newsId = UUID.randomUUID()
        val stockId = UUID.randomUUID()
        val homeData =
            UserHomeData(
                agents =
                    listOf(
                        UserHomeAgent(UUID.randomUUID(), AgentType.PRO, 4),
                        UserHomeAgent(UUID.randomUUID(), AgentType.ROOKIE, 3),
                        UserHomeAgent(UUID.randomUUID(), AgentType.TANKER, 2),
                    ),
                todayDecisionCount = 2,
                batchTime = batchTime,
                newsCards =
                    listOf(
                        UserHomeNewsCard(
                            cardId = cardId,
                            headline = "삼성전자, 반도체 실적 개선 기대",
                            newsId = newsId,
                            publishedAt = LocalDateTime.of(2026, 7, 21, 7, 30),
                            source = NewsSource.NAVER,
                            stockId = stockId,
                            stockName = "삼성전자",
                            changeRate = BigDecimal("1.3"),
                        ),
                    ),
            )
        `when`(userRepository.findByPublicId(userPublicId)).thenReturn(user)
        `when`(
            userHomeQueryRepository.getUserHomeData(
                7L,
                LocalDateTime.of(2026, 7, 21, 0, 0),
                LocalDateTime.of(2026, 7, 22, 0, 0),
            ),
        ).thenReturn(homeData)

        val response = userService.getUserHome(userPublicId)

        assertEquals("brifo", response.user.nickname)
        assertEquals(1250, response.user.balanceAp)
        assertEquals(listOf(AgentType.ROOKIE, AgentType.TANKER, AgentType.PRO), response.agents.map { it.agentType })
        assertEquals(2, response.todayDecisions.count)
        assertEquals(batchTime, response.todayNewsCards.batchTime)
        assertEquals(cardId, response.todayNewsCards.items.single().cardId)
        assertEquals(newsId, response.todayNewsCards.items.single().news.newsId)
        assertEquals(stockId, response.todayNewsCards.items.single().stock.stockId)
        assertEquals(BigDecimal("1.3"), response.todayNewsCards.items.single().stock.changeRate)
    }

    @Test
    fun `오늘 완료된 배치가 없으면 카드뉴스 목록은 비어 있다`() {
        val userPublicId = UUID.randomUUID()
        val user = myPageUser()
        val homeData =
            UserHomeData(
                agents = requiredAgents(),
                todayDecisionCount = 0,
                batchTime = null,
                newsCards = emptyList(),
            )
        `when`(userRepository.findByPublicId(userPublicId)).thenReturn(user)
        `when`(
            userHomeQueryRepository.getUserHomeData(
                7L,
                LocalDateTime.of(2026, 7, 21, 0, 0),
                LocalDateTime.of(2026, 7, 22, 0, 0),
            ),
        ).thenReturn(homeData)

        val response = userService.getUserHome(userPublicId)

        assertEquals(null, response.todayNewsCards.batchTime)
        assertEquals(emptyList<Any>(), response.todayNewsCards.items)
    }

    @Test
    fun `필수 에이전트가 누락되면 데이터 무결성 오류가 발생한다`() {
        val userPublicId = UUID.randomUUID()
        val user = myPageUser()
        val homeData =
            UserHomeData(
                agents = requiredAgents().dropLast(1),
                todayDecisionCount = 0,
                batchTime = null,
                newsCards = emptyList(),
            )
        `when`(userRepository.findByPublicId(userPublicId)).thenReturn(user)
        `when`(
            userHomeQueryRepository.getUserHomeData(
                7L,
                LocalDateTime.of(2026, 7, 21, 0, 0),
                LocalDateTime.of(2026, 7, 22, 0, 0),
            ),
        ).thenReturn(homeData)

        assertThrows(IllegalStateException::class.java) {
            userService.getUserHome(userPublicId)
        }
    }

    @Test
    fun `프로필 수정은 입력값을 trim하고 기존 관심 종목은 유지하며 목록을 교체한다`() {
        val userPublicId = UUID.randomUUID()
        val user = user()
        val removedStock = stock(UUID.randomUUID())
        val keptStock = stock(UUID.randomUUID())
        val addedStock = stock(UUID.randomUUID())
        val removedInterest = UserStock.create(user, removedStock)
        val keptInterest = UserStock.create(user, keptStock)
        val stockIds = listOf(requireNotNull(keptStock.publicId), requireNotNull(addedStock.publicId))
        val stocks = stockIds.map(::stock)
        `when`(userRepository.findByPublicId(userPublicId)).thenReturn(user)
        `when`(stockRepository.findAllByPublicIdInAndIsActiveTrue(stockIds)).thenReturn(stocks)
        `when`(userStockRepository.findAllByUser(user)).thenReturn(listOf(removedInterest, keptInterest))

        userService.updateUserProfile(
            userPublicId,
            UpdateUserProfileRequest(
                nickname = "  brifo  ",
                companyName = "  새 투자회사  ",
                stockIds = stockIds,
            ),
        )

        assertEquals("brifo", user.nickname)
        assertEquals("새 투자회사", user.companyName)
        verify(userStockRepository).deleteAllInBatch(listOf(removedInterest))
        @Suppress("UNCHECKED_CAST")
        val savedInterestsCaptor = ArgumentCaptor.forClass(List::class.java) as ArgumentCaptor<List<UserStock>>
        verify(userStockRepository).saveAll(savedInterestsCaptor.capture())
        assertEquals(listOf(addedStock.publicId), savedInterestsCaptor.value.map { it.stock.publicId })
    }

    @Test
    fun `프로필 수정 관심 종목은 최소 한 개여야 한다`() {
        assertThrows(StockSelectionMinimumNotMetException::class.java) {
            userService.updateUserProfile(
                UUID.randomUUID(),
                UpdateUserProfileRequest("brifo", "내 투자회사", emptyList()),
            )
        }
    }

    @Test
    fun `프로필 수정 관심 종목은 최대 세 개까지 허용한다`() {
        assertThrows(StockSelectionMaximumExceededException::class.java) {
            userService.updateUserProfile(
                UUID.randomUUID(),
                UpdateUserProfileRequest("brifo", "내 투자회사", List(4) { UUID.randomUUID() }),
            )
        }
    }

    @Test
    fun `프로필 수정 관심 종목에 중복이 있으면 예외를 던진다`() {
        val stockId = UUID.randomUUID()

        assertThrows(DuplicatedStockSelectionException::class.java) {
            userService.updateUserProfile(
                UUID.randomUUID(),
                UpdateUserProfileRequest("brifo", "내 투자회사", listOf(stockId, stockId)),
            )
        }
    }

    @Test
    fun `프로필 수정 요청의 종목을 찾을 수 없으면 예외를 던진다`() {
        val userPublicId = UUID.randomUUID()
        val user = user()
        val stockIds = listOf(UUID.randomUUID(), UUID.randomUUID())
        val existingStock = stock(stockIds.first())
        `when`(userRepository.findByPublicId(userPublicId)).thenReturn(user)
        `when`(stockRepository.findAllByPublicIdInAndIsActiveTrue(stockIds)).thenReturn(listOf(existingStock))

        assertThrows(StockNotFoundException::class.java) {
            userService.updateUserProfile(
                userPublicId,
                UpdateUserProfileRequest("brifo", "내 투자회사", stockIds),
            )
        }

        verifyNoInteractions(userStockRepository)
    }

    @Test
    fun `회원 탈퇴는 현재 사용자를 soft delete한다`() {
        val userPublicId = UUID.randomUUID()
        val user = user()
        `when`(userRepository.findByPublicId(userPublicId)).thenReturn(user)

        userService.deleteUser(userPublicId)

        verify(userRepository).delete(user)
    }

    @Test
    fun `회원 탈퇴 사용자를 찾을 수 없으면 예외를 던진다`() {
        val userPublicId = UUID.randomUUID()
        `when`(userRepository.findByPublicId(userPublicId)).thenReturn(null)

        assertThrows(UserNotFoundException::class.java) {
            userService.deleteUser(userPublicId)
        }
    }

    @Test
    fun `온보딩 완료는 완료 시각을 기록하고 기본 에이전트 세 개를 생성한다`() {
        val user = user().also { it.updateOnboardingProfile("brifo", null) }
        `when`(userRepository.findByProviderAndSocialId(OAuthProvider.KAKAO, "social-id")).thenReturn(user)
        `when`(policyRepository.countByIsRequiredTrueAndIsActiveTrue()).thenReturn(2)
        `when`(userPolicyRepository.countActiveRequiredAgreements(user)).thenReturn(2)
        `when`(userStockRepository.countByUser(user)).thenReturn(3)
        `when`(agentRepository.existsByUser(user)).thenReturn(false)

        userService.completeOnboarding(OAuthProvider.KAKAO, "social-id")

        assertEquals(LocalDateTime.of(2026, 7, 21, 18, 0), user.onboardingCompletedAt)
        @Suppress("UNCHECKED_CAST")
        val agentsCaptor = ArgumentCaptor.forClass(List::class.java) as ArgumentCaptor<List<Agent>>
        verify(agentRepository).saveAll(agentsCaptor.capture())
        assertEquals(
            listOf(AgentType.ROOKIE, AgentType.TANKER, AgentType.PRO),
            agentsCaptor.value.map { it.agentType },
        )
    }

    @Test
    fun `인증 연동 전에는 공개 ID로 사용자를 조회해 온보딩을 완료한다`() {
        val userPublicId = UUID.randomUUID()
        val user = user().also { it.updateOnboardingProfile("brifo", null) }
        `when`(userRepository.findByPublicId(userPublicId)).thenReturn(user)
        `when`(policyRepository.countByIsRequiredTrueAndIsActiveTrue()).thenReturn(2)
        `when`(userPolicyRepository.countActiveRequiredAgreements(user)).thenReturn(2)
        `when`(userStockRepository.countByUser(user)).thenReturn(1)
        `when`(agentRepository.existsByUser(user)).thenReturn(false)

        userService.completeOnboarding(userPublicId)

        verify(userRepository).findByPublicId(userPublicId)
        assertEquals(LocalDateTime.of(2026, 7, 21, 18, 0), user.onboardingCompletedAt)
    }

    @Test
    fun `온보딩 완료 조건은 필수 약관 동의를 가장 먼저 검증한다`() {
        val user = user()
        `when`(userRepository.findByProviderAndSocialId(OAuthProvider.KAKAO, "social-id")).thenReturn(user)
        `when`(policyRepository.countByIsRequiredTrueAndIsActiveTrue()).thenReturn(2)
        `when`(userPolicyRepository.countActiveRequiredAgreements(user)).thenReturn(1)

        assertThrows(RequiredPoliciesNotAgreedException::class.java) {
            userService.completeOnboarding(OAuthProvider.KAKAO, "social-id")
        }

        verifyNoInteractions(agentRepository)
    }

    @Test
    fun `필수 약관에 동의했지만 닉네임이 없으면 프로필 미완료 예외를 던진다`() {
        val user = user()
        `when`(userRepository.findByProviderAndSocialId(OAuthProvider.KAKAO, "social-id")).thenReturn(user)
        `when`(policyRepository.countByIsRequiredTrueAndIsActiveTrue()).thenReturn(2)
        `when`(userPolicyRepository.countActiveRequiredAgreements(user)).thenReturn(2)

        assertThrows(OnboardingProfileNotCompletedException::class.java) {
            userService.completeOnboarding(OAuthProvider.KAKAO, "social-id")
        }

        verifyNoInteractions(agentRepository)
    }

    @Test
    fun `프로필을 입력했지만 관심 종목이 없으면 종목 미선택 예외를 던진다`() {
        val user = user().also { it.updateOnboardingProfile("brifo", null) }
        `when`(userRepository.findByProviderAndSocialId(OAuthProvider.KAKAO, "social-id")).thenReturn(user)
        `when`(policyRepository.countByIsRequiredTrueAndIsActiveTrue()).thenReturn(2)
        `when`(userPolicyRepository.countActiveRequiredAgreements(user)).thenReturn(2)
        `when`(userStockRepository.countByUser(user)).thenReturn(0)

        assertThrows(OnboardingStocksNotSelectedException::class.java) {
            userService.completeOnboarding(OAuthProvider.KAKAO, "social-id")
        }

        verifyNoInteractions(agentRepository)
    }

    @Test
    fun `이미 완료된 온보딩을 다시 완료할 수 없다`() {
        val user = user().also { it.completeOnboarding(LocalDateTime.of(2026, 7, 20, 10, 0)) }
        `when`(userRepository.findByProviderAndSocialId(OAuthProvider.KAKAO, "social-id")).thenReturn(user)

        assertThrows(OnboardingAlreadyCompletedException::class.java) {
            userService.completeOnboarding(OAuthProvider.KAKAO, "social-id")
        }

        verifyNoInteractions(policyRepository, userPolicyRepository, agentRepository)
    }

    private fun user(): User =
        User.create(
            provider = OAuthProvider.KAKAO,
            socialId = "social-id",
            email = "brifo@example.com",
        )

    private fun myPageUser(): User =
        mock(User::class.java).also {
            `when`(it.id).thenReturn(7L)
            `when`(it.nickname).thenReturn("brifo")
            `when`(it.companyName).thenReturn("내 투자회사")
            `when`(it.balanceAp).thenReturn(1250)
        }

    private fun requiredAgents(): List<UserHomeAgent> =
        listOf(
            UserHomeAgent(UUID.randomUUID(), AgentType.ROOKIE, 1),
            UserHomeAgent(UUID.randomUUID(), AgentType.TANKER, 1),
            UserHomeAgent(UUID.randomUUID(), AgentType.PRO, 1),
        )

    private fun stock(publicId: UUID): Stock =
        mock(Stock::class.java).also {
            `when`(it.publicId).thenReturn(publicId)
        }
}
