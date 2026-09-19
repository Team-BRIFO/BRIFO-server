package com.brifo.server.auth.service

import com.brifo.server.auth.dto.internal.OAuthUserProfile
import com.brifo.server.auth.dto.response.OAuthLoginResponse
import com.brifo.server.user.entity.OAuthProvider
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * 카카오/네이버 없이 서비스를 체험할 수 있도록, 호출마다 독립된 게스트 계정을 발급한다.
 * socialId를 매번 새로 생성하므로 항상 신규 회원으로 취급되어 온보딩 절차를 그대로 거친다.
 */
@Service
class GuestLoginService(
    private val oauthLoginService: OAuthLoginService,
) {
    fun login(): OAuthLoginResponse {
        val profile =
            OAuthUserProfile(
                provider = OAuthProvider.GUEST,
                socialId = UUID.randomUUID().toString(),
                email = null,
                nickname = null,
            )
        return oauthLoginService.login(profile)
    }
}
