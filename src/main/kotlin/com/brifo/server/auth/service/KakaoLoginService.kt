package com.brifo.server.auth.service

import com.brifo.server.ap.entity.ApTransaction
import com.brifo.server.ap.entity.ApTransactionReason
import com.brifo.server.ap.repository.ApTransactionRepository
import com.brifo.server.auth.dto.KakaoLoginRequest
import com.brifo.server.auth.dto.KakaoLoginResponse
import com.brifo.server.auth.dto.UserInfo
import com.brifo.server.user.entity.OAuthProvider
import com.brifo.server.user.entity.User
import com.brifo.server.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
class KakaoLoginService(
    private val kakaoApiClient: KakaoApiClient,
    private val userRepository: UserRepository,
    private val apTransactionRepository: ApTransactionRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val clock: Clock,
) {
    @Transactional
    fun login(request: KakaoLoginRequest): KakaoLoginResponse {
        val kakaoUser = kakaoApiClient.getUser(request.authorizationCode, request.redirectUri)
        val socialId = kakaoUser.id.toString()
        val existingUser = userRepository.findByProviderAndSocialId(OAuthProvider.KAKAO, socialId)
        val user = existingUser ?: createPendingUser(socialId, kakaoUser.kakaoAccount?.email, kakaoUser.kakaoAccount?.profile?.nickname)

        if (user.onboardingCompletedAt == null) {
            return signupRequired(user)
        }

        val publicId = requireNotNull(user.publicId) { "Persisted user must have a publicId." }
        user.markLoggedIn(LocalDateTime.now(clock))
        return KakaoLoginResponse.Login(
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
                provider = OAuthProvider.KAKAO,
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

    private fun signupRequired(user: User): KakaoLoginResponse.SignupRequired =
        KakaoLoginResponse.SignupRequired(
            signupToken =
                jwtTokenProvider.issueSignupToken(
                    provider = user.provider,
                    socialId = user.socialId,
                    email = user.email,
                ),
        )

    companion object {
        private const val INITIAL_AP = 500
        private const val DEFAULT_NICKNAME = "카카오 사용자"
    }
}
