package com.brifo.server.news.client

import com.brifo.server.externalapi.ExternalApiCallContext
import com.brifo.server.externalapi.ExternalApiCallPolicy
import com.brifo.server.externalapi.ExternalApiCallService
import com.brifo.server.externalapi.dataserver.DataServerDisclosureResult
import com.brifo.server.externalapi.dataserver.DataServerResponse
import com.brifo.server.externalapi.idempotency.ExternalApiIdempotencyKeyGenerator
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class DataServerDisclosureClient(
    @Qualifier("dataServerRestClient") private val restClient: RestClient,
    private val externalApiCallService: ExternalApiCallService,
) : DisclosureClient {
    override fun exists(request: DisclosureClient.Request): Boolean {
        val response = externalApiCallService.execute(
            provider = "DATA_SERVER", apiName = "DISCLOSURE", policy = ExternalApiCallPolicy.DISCLOSURE,
            retryEnabled = true,
            context = ExternalApiCallContext(
                idempotencyKey = ExternalApiIdempotencyKeyGenerator.disclosure(request.stockCode, request.date),
                stockId = request.stockId,
            ),
            requestPayload = request,
        ) {
            restClient.get().uri { it.path("/api/stocks/{stockCode}/disclosures/exists").queryParam("date", request.date).build(request.stockCode) }
                .retrieve().toEntity(object : ParameterizedTypeReference<DataServerResponse<DataServerDisclosureResult>>() {})
                .also { entity ->
                    val body = checkNotNull(entity.body) { "데이터 서버 공시 응답 본문이 없습니다." }
                    check(body.success) { "${body.code}: ${body.message}" }
                    checkNotNull(body.result) { "${body.code}: 공시 조회 결과가 없습니다." }
                }
        }
        return checkNotNull(response.result).disclosure.hasDisclosure
    }
}
