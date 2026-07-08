package com.brifo.server.auth.service

import com.brifo.server.auth.dto.KakaoInfo
import com.brifo.server.auth.dto.KakaoLoginRequest
import com.brifo.server.auth.dto.KakaoLoginResponse
import com.brifo.server.auth.dto.UserInfo
import com.brifo.server.user.entity.OAuthProvider
import com.brifo.server.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
class KakaoLoginService(
    private val kakaoApiClient: KakaoApiClient,
    private val userRepository: UserRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val clock: Clock,
) {
    @Transactional
    fun login(request: KakaoLoginRequest): KakaoLoginResponse {
        val kakaoUser = kakaoApiClient.getUser(request.authorizationCode, request.redirectUri)
        val socialId = kakaoUser.id.toString()
        val user = userRepository.findByProviderAndSocialId(OAuthProvider.KAKAO, socialId)

        if (user == null) {
            val kakaoInfo = KakaoInfo(id = socialId, email = kakaoUser.kakaoAccount?.email)
            return KakaoLoginResponse.SignupRequired(
                signupToken = jwtTokenProvider.issueSignupToken(kakaoInfo),
                kakaoInfo = kakaoInfo,
            )
        }

        val publicId = requireNotNull(user.publicId) { "Persisted user must have a publicId." }
        user.markLoggedIn(LocalDateTime.now(clock))
        return KakaoLoginResponse.Login(
            user = UserInfo(userId = publicId, nickname = user.nickname, email = user.email),
            token = jwtTokenProvider.issueLoginTokens(publicId),
        )
    }
}
