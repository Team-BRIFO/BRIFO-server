package com.brifo.server.auth.service

import com.brifo.server.ap.entity.ApTransaction
import com.brifo.server.ap.entity.ApTransactionReason
import com.brifo.server.ap.repository.ApTransactionRepository
import com.brifo.server.auth.dto.internal.OAuthUserProfile
import com.brifo.server.auth.dto.response.OAuthLoginResponse
import com.brifo.server.auth.dto.response.UserInfo
import com.brifo.server.user.entity.User
import com.brifo.server.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
class OAuthLoginService(
    private val userRepository: UserRepository,
    private val apTransactionRepository: ApTransactionRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val clock: Clock,
) {
    @Transactional
    fun login(profile: OAuthUserProfile): OAuthLoginResponse {
        val existingUser = userRepository.findByProviderAndSocialId(profile.provider, profile.socialId)
        val user = existingUser ?: createPendingUser(profile)

        if (user.onboardingCompletedAt == null) {
            return signupRequired(user)
        }

        val publicId = requireNotNull(user.publicId) { "Persisted user must have a publicId." }
        user.markLoggedIn(LocalDateTime.now(clock))
        return OAuthLoginResponse.Login(
            user = UserInfo(userId = publicId, nickname = requireNotNull(user.nickname), email = user.email),
            token = jwtTokenProvider.issueLoginTokens(publicId),
        )
    }

    private fun createPendingUser(profile: OAuthUserProfile): User {
        val user =
            User.create(
                provider = profile.provider,
                socialId = profile.socialId,
                nickname = profile.nickname?.takeIf { it.isNotBlank() },
                email = profile.email?.takeIf { it.isNotBlank() },
            )
        user.grantAp(INITIAL_AP)
        userRepository.saveAndFlush(user)
        apTransactionRepository.save(
            ApTransaction.create(
                user = user,
                amount = INITIAL_AP,
                reason = ApTransactionReason.INITIAL_GRANT,
            ),
        )
        return user
    }

    private fun signupRequired(user: User): OAuthLoginResponse.SignupRequired =
        OAuthLoginResponse.SignupRequired(
            signupToken = jwtTokenProvider.issueSignupToken(requireNotNull(user.publicId)),
        )

    companion object {
        private const val INITIAL_AP = 500
    }
}
