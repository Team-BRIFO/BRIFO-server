package com.brifo.server.auth.service

import com.brifo.server.ap.entity.ApTransaction
import com.brifo.server.ap.entity.ApTransactionReason
import com.brifo.server.ap.repository.ApTransactionRepository
import com.brifo.server.auth.client.NaverApiClient
import com.brifo.server.auth.dto.request.NaverLoginRequest
import com.brifo.server.auth.dto.response.NaverLoginResponse
import com.brifo.server.auth.dto.response.UserInfo
import com.brifo.server.user.entity.OAuthProvider
import com.brifo.server.user.entity.User
import com.brifo.server.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
class NaverLoginService(
    private val naverApiClient: NaverApiClient,
    private val userRepository: UserRepository,
    private val apTransactionRepository: ApTransactionRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val clock: Clock,
) {
    @Transactional
    fun login(request: NaverLoginRequest): NaverLoginResponse {
        val naverUser = naverApiClient.getUser(request.authorizationCode, request.state, request.redirectUri)
        val existingUser = userRepository.findByProviderAndSocialId(OAuthProvider.NAVER, naverUser.id)
        val user = existingUser ?: createPendingUser(naverUser.id, naverUser.email, naverUser.nickname)

        if (user.onboardingCompletedAt == null) {
            return signupRequired(user)
        }

        val publicId = requireNotNull(user.publicId) { "Persisted user must have a publicId." }
        user.markLoggedIn(LocalDateTime.now(clock))
        return NaverLoginResponse.Login(
            user = UserInfo(userId = publicId, nickname = requireNotNull(user.nickname), email = user.email),
            token = jwtTokenProvider.issueLoginTokens(publicId),
        )
    }

    private fun createPendingUser(
        socialId: String,
        email: String?,
        nickname: String?,
    ): User {
        val user =
            User.create(
                provider = OAuthProvider.NAVER,
                socialId = socialId,
                nickname = nickname?.takeIf { it.isNotBlank() } ?: DEFAULT_NICKNAME,
                email = email,
            )
        user.grantAp(INITIAL_AP)
        userRepository.save(user)
        apTransactionRepository.save(
            ApTransaction.create(
                user = user,
                amount = INITIAL_AP,
                reason = ApTransactionReason.INITIAL_GRANT,
            ),
        )
        return user
    }

    private fun signupRequired(user: User): NaverLoginResponse.SignupRequired =
        NaverLoginResponse.SignupRequired(
            signupToken =
                jwtTokenProvider.issueSignupToken(
                    provider = user.provider,
                    socialId = user.socialId,
                    email = user.email,
                ),
        )

    companion object {
        private const val INITIAL_AP = 500
        private const val DEFAULT_NICKNAME = "네이버 사용자"
    }
}
