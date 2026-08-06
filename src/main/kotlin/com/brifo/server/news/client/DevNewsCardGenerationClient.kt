package com.brifo.server.news.client

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("dev")
class DevNewsCardGenerationClient : NewsCardGenerationClient {
    // TODO: 외부 카드뉴스 생성 API 구현체로 교체한다.
    override fun createNewsCard(request: NewsCardGenerationClient.Request): NewsCardGenerationClient.Result =
        throw UnsupportedOperationException("외부 카드뉴스 생성 API가 아직 연결되지 않았습니다.")
}
