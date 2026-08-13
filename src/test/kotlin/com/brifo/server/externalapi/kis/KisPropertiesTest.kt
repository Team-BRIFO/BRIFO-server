package com.brifo.server.externalapi.kis

import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class KisPropertiesTest {
    @Test
    fun `키가 주입되지 않으면 기동에 실패한다`() {
        // application.yaml 기본값. 이대로 두면 부팅은 되고 시세 호출만 401로 조용히 실패한다.
        val exception =
            assertFailsWith<IllegalArgumentException> {
                KisProperties(baseUrl = BASE_URL, appKey = "disabled", appSecret = SECRET)
            }

        assertTrue(exception.message!!.contains("KIS_APP_KEY"), "누락된 환경변수명을 알려줘야 한다.")
    }

    @Test
    fun `빈 값도 거부한다`() {
        assertFailsWith<IllegalArgumentException> {
            KisProperties(baseUrl = BASE_URL, appKey = KEY, appSecret = "   ")
        }
        assertFailsWith<IllegalArgumentException> {
            KisProperties(baseUrl = "", appKey = KEY, appSecret = SECRET)
        }
    }

    @Test
    fun `모든 값이 주입되면 정상 생성된다`() {
        KisProperties(baseUrl = BASE_URL, appKey = KEY, appSecret = SECRET)
    }

    private companion object {
        const val BASE_URL = "https://openapi.koreainvestment.com:9443"
        const val KEY = "test-app-key"
        const val SECRET = "test-app-secret"
    }
}
