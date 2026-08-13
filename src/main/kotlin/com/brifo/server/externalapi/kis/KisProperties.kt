package com.brifo.server.externalapi.kis

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "external.kis")
data class KisProperties(
    val baseUrl: String,
    val appKey: String,
    val appSecret: String,
) {
    /**
     * 이 프로퍼티는 `DATA_PROVIDER=kis`일 때만 바인딩되므로(`ExternalRestClientConfig`),
     * 여기서 값이 비어 있다는 건 KIS를 쓰겠다고 해놓고 키를 안 넘겼다는 뜻이다.
     *
     * 미주입 시 `application.yaml`의 기본값 `disabled`가 들어와 부팅은 성공하고
     * 모든 시세 호출만 401로 실패한다. 헬스체크는 UP이라 원인 파악이 오래 걸리므로
     * 기동 시점에 실패시킨다.
     */
    init {
        requireConfigured(baseUrl, "KIS_BASE_URL")
        requireConfigured(appKey, "KIS_APP_KEY")
        requireConfigured(appSecret, "KIS_APP_SECRET")
    }

    private fun requireConfigured(
        value: String,
        envName: String,
    ) {
        require(value.isNotBlank() && value != PLACEHOLDER) {
            "DATA_PROVIDER=kis 인데 $envName 이(가) 주입되지 않았습니다. (현재 값: '$value')"
        }
    }

    private companion object {
        const val PLACEHOLDER = "disabled"
    }
}
