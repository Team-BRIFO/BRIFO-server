package com.brifo.server.externalapi.naver

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "external.naver")
data class NaverNewsProperties(
    val baseUrl: String = "https://naverapihub.apigw.ntruss.com",
    val clientId: String = "disabled",
    val clientSecret: String = "disabled",
    val connectTimeout: Duration = Duration.ofSeconds(3),
    val readTimeout: Duration = Duration.ofSeconds(5),
    /**
     * 네이버에 요청할 검색 결과 수(API `display`, 최대 100).
     *
     * 제목에 종목 표기가 없는 기사를 걸러낸 뒤 `collectCount`만큼만 남기므로 넉넉히 받는다.
     * 이 값을 줄이면 관련 기사가 다 걸러졌을 때 수집 건수가 0이 될 수 있다.
     */
    val searchDisplayCount: Int = 20,
    /** 관련 기사 중 실제로 수집할 종목당 건수. 종목당 호출은 1회라 이 값과 API 호출 수는 무관하다. */
    val collectCount: Int = 3,
)
