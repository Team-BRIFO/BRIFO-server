package com.brifo.server.auth.dev

import com.brifo.server.auth.dto.internal.OAuthUserProfile
import com.brifo.server.auth.dto.response.OAuthLoginResponse
import com.brifo.server.auth.service.OAuthLoginService
import com.brifo.server.global.code.ErrorCode
import com.brifo.server.global.exception.BusinessException
import com.brifo.server.policy.entity.Policy
import com.brifo.server.policy.entity.PolicyCode
import com.brifo.server.policy.repository.PolicyRepository
import com.brifo.server.policy.service.PolicyService
import com.brifo.server.stock.entity.Stock
import com.brifo.server.stock.repository.StockRepository
import com.brifo.server.user.dto.request.UpdateOnboardingProfileRequest
import com.brifo.server.user.dto.response.CompleteOnboardingResponse
import com.brifo.server.user.entity.OAuthProvider
import com.brifo.server.user.repository.UserRepository
import com.brifo.server.user.service.UserService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID

@Service
@Profile("dev")
@ConditionalOnProperty(prefix = "app.dev-auth", name = ["enabled"], havingValue = "true")
class DevAuthService(
    private val properties: DevAuthProperties,
    private val oauthLoginService: OAuthLoginService,
    private val policyRepository: PolicyRepository,
    private val policyService: PolicyService,
    private val stockRepository: StockRepository,
    private val userRepository: UserRepository,
    private val userService: UserService,
) {
    @Transactional
    fun signUp(request: DevSignUpRequest): DevSignUpResponse {
        verifyPassword(request.password)

        val identifier = UUID.randomUUID().toString()
        val result =
            oauthLoginService.login(
                OAuthUserProfile(
                    provider = OAuthProvider.KAKAO,
                    socialId = "$DEV_SOCIAL_ID_PREFIX$identifier",
                    email = "$identifier@dev.invalid",
                    nickname = null,
                ),
            )

        check(result is OAuthLoginResponse.SignupRequired)

        return DevSignUpResponse(signupToken = result.signupToken)
    }

    @Transactional
    fun completeOnboarding(userPublicId: UUID): CompleteOnboardingResponse {
        val user = userRepository.findByPublicId(userPublicId)
        if (user == null || !user.socialId.startsWith(DEV_SOCIAL_ID_PREFIX)) {
            throw BusinessException(ErrorCode.FORBIDDEN)
        }

        val stock =
            stockRepository.findAll().firstOrNull { it.isActive }
                ?: stockRepository.saveAndFlush(createDevStock())
        val activePolicies =
            policyRepository.findAll().filter { it.isActive }.ifEmpty {
                listOf(policyRepository.saveAndFlush(createDevPolicy()))
            }

        userService.updateOnboardingProfile(
            userPublicId,
            UpdateOnboardingProfileRequest(
                nickname = DEV_PROFILE_VALUE,
                companyName = DEV_PROFILE_VALUE,
                stockIds = listOf(requireNotNull(stock.publicId)),
            ),
        )
        policyService.agreePolicies(userPublicId, activePolicies.map { requireNotNull(it.publicId) })
        return userService.completeOnboarding(userPublicId)
    }

    private fun createDevPolicy(): Policy =
        Policy.create(
            code = PolicyCode.TERMS_OF_SERVICE,
            title = DEV_PROFILE_VALUE,
            content = DEV_PROFILE_VALUE,
            isRequired = true,
        )

    private fun createDevStock(): Stock =
        Stock.create(
            code = "DEV${UUID.randomUUID().toString().replace("-", "").take(7)}",
            name = DEV_PROFILE_VALUE,
            sector = DEV_PROFILE_VALUE,
        )

    private fun verifyPassword(actual: String) {
        val matches =
            MessageDigest.isEqual(
                properties.password.toByteArray(StandardCharsets.UTF_8),
                actual.toByteArray(StandardCharsets.UTF_8),
            )
        if (!matches) {
            throw BusinessException(ErrorCode.UNAUTHORIZED)
        }
    }

    companion object {
        private const val DEV_PROFILE_VALUE = "test"
        private const val DEV_SOCIAL_ID_PREFIX = "dev:"
    }
}
