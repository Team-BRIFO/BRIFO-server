package com.brifo.server.news.client

import com.brifo.server.externalapi.ExternalApiCallContext
import com.brifo.server.externalapi.ExternalApiCallPolicy
import com.brifo.server.externalapi.ExternalApiCallService
import com.brifo.server.externalapi.dataserver.DataServerNewsResult
import com.brifo.server.externalapi.dataserver.DataServerResponse
import com.brifo.server.externalapi.idempotency.ExternalApiIdempotencyKeyGenerator
import com.brifo.server.news.entity.NewsSource
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class DataServerNewsCollectionClient(
    @Qualifier("dataServerRestClient") private val restClient: RestClient,
    private val externalApiCallService: ExternalApiCallService,
) : NewsCollectionClient {
    override fun collect(request: NewsCollectionClient.Request): NewsCollectionClient.Result {
        val news = request.stockCodes.asSequence().flatMap { stockCode ->
            val response = externalApiCallService.execute(
                provider = "DATA_SERVER", apiName = "NEWS_COLLECTION", policy = ExternalApiCallPolicy.NEWS_COLLECTION,
                retryEnabled = true,
                context = ExternalApiCallContext(
                    idempotencyKey = ExternalApiIdempotencyKeyGenerator.newsCollection(stockCode, request.targetDate),
                ),
                requestPayload = mapOf("stockCode" to stockCode, "date" to request.targetDate),
            ) {
                restClient.get().uri { it.path("/api/stocks/{stockCode}/news").queryParam("date", request.targetDate).build(stockCode) }
                    .retrieve().toEntity(object : ParameterizedTypeReference<DataServerResponse<DataServerNewsResult>>() {})
                    .also { entity ->
                        val body = checkNotNull(entity.body) { "데이터 서버 뉴스 응답 본문이 없습니다." }
                        check(body.success) { "${body.code}: ${body.message}" }
                        checkNotNull(body.result) { "${body.code}: 뉴스 조회 결과가 없습니다." }
                    }
            }
            checkNotNull(response.result).news.asSequence().map { item ->
                NewsCollectionClient.CollectedNews(
                    stockCode = stockCode,
                    source = NewsSource.DATA_SERVER,
                    sourceUrl = item.sourceUrl,
                    sourceImageUrl = item.imageUrl,
                    title = item.title,
                    summary = item.content,
                    dedupKey = "data-server:${item.newsId}",
                    publishedAt = item.publishedAt,
                )
            }
        }.toList()
        return NewsCollectionClient.Result(news)
    }
}
