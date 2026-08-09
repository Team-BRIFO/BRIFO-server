package com.brifo.server.news.client

import com.brifo.server.externalapi.ExternalApiCallPolicy
import com.brifo.server.externalapi.ExternalApiCallService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class AiNewsCardGenerationClient(
    @Qualifier("aiNewsCardRestClient")
    private val restClient: RestClient,
    private val externalApiCallService: ExternalApiCallService,
) : NewsCardGenerationClient {
    override fun createNewsCard(
        request: NewsCardGenerationClient.Request,
    ): NewsCardGenerationClient.Result {
        val response = externalApiCallService.execute(
            provider = "AI",
            apiName = "CREATE_CARD_NEWS",
            policy = ExternalApiCallPolicy.AI_CARD_NEWS,

            // newsContent는 공통 로그에서 자동으로 마스킹된다.
            requestPayload = request,
        ) {
            restClient
                .post()
                .uri("/ai/news/summarize") // AI 측 실제 API 경로로 변경
                .body(request)
                .retrieve()
                .toEntity(AiResponse::class.java)
                .also { entity ->
                    val body = checkNotNull(entity.body) { "AI 카드뉴스 응답 본문이 없습니다." }
                    check(body.isSuccess) { "${body.code}: ${body.message}" }
                    checkNotNull(body.result) { "${body.code}: 카드뉴스 생성 결과가 없습니다." }
                }
        }
        return checkNotNull(response.result)
    }

    private data class AiResponse(
        val isSuccess: Boolean,
        val code: String,
        val message: String,
        val result: NewsCardGenerationClient.Result?,
    )
}
