package com.brifo.server.news.client

import com.brifo.server.externalapi.ExternalApiCallPolicy
import com.brifo.server.externalapi.ExternalApiCallService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class AiCardNewsClient(
    @Qualifier("aiRestClient")
    private val restClient: RestClient,
    private val externalApiCallService: ExternalApiCallService,
) : CardNewsClient {
    override fun createCardNews(
        request: CardNewsClient.Request,
    ): CardNewsClient.Response {
        return externalApiCallService.execute(
            provider = "AI",
            apiName = "CREATE_CARD_NEWS",
            policy = ExternalApiCallPolicy.FAST_API,

            // newsContent는 공통 로그에서 자동으로 마스킹된다.
            requestPayload = request,
        ) {
            restClient
                .post()
                .uri("/ai/news/summarize") // AI 측 실제 API 경로로 변경
                .body(request)
                .retrieve()
                .toEntity(CardNewsClient.Response::class.java)
        }
    }
}
