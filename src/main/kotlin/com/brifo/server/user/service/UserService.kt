package com.brifo.server.user.service

import com.brifo.server.user.dto.request.UpdateOnboardingProfileRequest
import com.brifo.server.user.entity.OAuthProvider
import com.brifo.server.user.exception.InvalidCompanyNameException
import com.brifo.server.user.exception.InvalidNicknameException
import com.brifo.server.user.exception.OnboardingAlreadyCompletedException
import com.brifo.server.user.exception.UserNotFoundException
import com.brifo.server.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
) {
    @Transactional
    fun updateOnboardingProfile(
        provider: OAuthProvider,
        socialId: String,
        request: UpdateOnboardingProfileRequest,
    ) {
        val nickname = validateNickname(request.nickname)
        val companyName = validateCompanyName(request.companyName)
        val user = userRepository.findByProviderAndSocialId(provider, socialId) ?: throw UserNotFoundException()

        if (user.onboardingCompletedAt != null) {
            throw OnboardingAlreadyCompletedException()
        }

        user.updateOnboardingProfile(nickname, companyName)
    }

    private fun validateNickname(nickname: String): String =
        nickname.trim().takeIf { it.length in NICKNAME_LENGTH_RANGE }
            ?: throw InvalidNicknameException()

    private fun validateCompanyName(companyName: String?): String? {
        if (companyName == null) return null

        return companyName.trim().takeIf { it.length in COMPANY_NAME_LENGTH_RANGE }
            ?: throw InvalidCompanyNameException()
    }

    companion object {
        private val NICKNAME_LENGTH_RANGE = 1..50
        private val COMPANY_NAME_LENGTH_RANGE = 1..100
    }
}
