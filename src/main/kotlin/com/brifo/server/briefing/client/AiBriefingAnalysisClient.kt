package com.brifo.server.briefing.client

import com.brifo.server.externalapi.ExternalApiCallPolicy
import com.brifo.server.externalapi.ExternalApiCallService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class AiBriefingAnalysisClient(
    @Qualifier("aiBriefingRestClient") private val restClient: RestClient,
    private val externalApiCallService: ExternalApiCallService,
) : BriefingAnalysisClient {
    override fun createBriefings(request: BriefingAnalysisClient.Request): BriefingAnalysisClient.Result {
        val response = externalApiCallService.execute(
            provider = "AI", apiName = "CREATE_BRIEFINGS", policy = ExternalApiCallPolicy.AI_BRIEFING,
            requestPayload = request,
        ) {
            restClient.post().uri("/ai/briefing/generate").body(request).retrieve().toEntity(AiResponse::class.java)
                .also { entity ->
                    val body = checkNotNull(entity.body) { "AI 브리핑 응답 본문이 없습니다." }
                    check(body.isSuccess) { "${body.code}: ${body.message}" }
                    checkNotNull(body.result) { "${body.code}: 브리핑 생성 결과가 없습니다." }
                }
        }
        return checkNotNull(response.result) { "브리핑 생성 결과가 없습니다." }
    }

    private data class AiResponse(
        val isSuccess: Boolean,
        val code: String,
        val message: String,
        val result: BriefingAnalysisClient.Result?,
    )
}
