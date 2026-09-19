package com.brifo.server.auth.service

import com.brifo.server.auth.dto.internal.OAuthUserProfile
import com.brifo.server.auth.dto.response.OAuthLoginResponse
import com.brifo.server.user.entity.OAuthProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class GuestLoginServiceTest {
    @Mock
    private lateinit var oauthLoginService: OAuthLoginService

    private lateinit var service: GuestLoginService

    @BeforeEach
    fun setUp() {
        service = GuestLoginService(oauthLoginService)
    }

    @Test
    fun `GUEST 제공자와 임의의 socialId로 공통 로그인 서비스에 위임한다`() {
        val expected = OAuthLoginResponse.SignupRequired(signupToken = "signup-token")
        `when`(oauthLoginService.login(anyProfile())).thenReturn(expected)

        val response = service.login()

        assertSame(expected, response)
        val profileCaptor = ArgumentCaptor.forClass(OAuthUserProfile::class.java)
        verify(oauthLoginService).login(captureProfile(profileCaptor))
        assertEquals(OAuthProvider.GUEST, profileCaptor.value.provider)
        assertNull(profileCaptor.value.nickname)
        assertNull(profileCaptor.value.email)
    }

    @Test
    fun `호출마다 서로 다른 socialId를 발급한다`() {
        `when`(oauthLoginService.login(anyProfile()))
            .thenReturn(OAuthLoginResponse.SignupRequired(signupToken = "signup-token"))

        service.login()
        service.login()

        val captor = ArgumentCaptor.forClass(OAuthUserProfile::class.java)
        verify(oauthLoginService, times(2)).login(captureProfile(captor))
        val (first, second) = captor.allValues
        assertNotEquals(first.socialId, second.socialId)
    }

    /**
     * OAuthLoginService.login()은 Kotlin에서 선언된 non-null 파라미터를 받기 때문에,
     * Mockito의 any()/capture()가 실제로 반환하는 null 값을 그대로 넘기면 Kotlin이 삽입하는
     * 제네릭 경계 null 체크에서 바로 NPE가 난다. 매처 등록은 그대로 하되(부수효과),
     * 실제 인자로는 명시적 unchecked cast로 만든 null을 넘겨 그 체크를 우회한다.
     */
    private fun anyProfile(): OAuthUserProfile {
        ArgumentMatchers.any(OAuthUserProfile::class.java)
        return uninitializedProfile()
    }

    private fun captureProfile(captor: ArgumentCaptor<OAuthUserProfile>): OAuthUserProfile {
        captor.capture()
        return uninitializedProfile()
    }

    private fun uninitializedProfile(): OAuthUserProfile = uninitialized()

    @Suppress("UNCHECKED_CAST")
    private fun <T> uninitialized(): T = null as T
}
