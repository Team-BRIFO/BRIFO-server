package com.brifo.server.news.client

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("dev")
class DevNewsCollectionClient : NewsCollectionClient {
    // TODO: 외부 뉴스 수집 API 구현체로 교체한다.
    override fun collect(request: NewsCollectionClient.Request): NewsCollectionClient.Result =
        throw UnsupportedOperationException("외부 뉴스 수집 API가 아직 연결되지 않았습니다.")
}
