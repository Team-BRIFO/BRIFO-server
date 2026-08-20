package com.brifo.server.news.client

import com.brifo.server.externalapi.ExternalApiCallContext
import com.brifo.server.externalapi.ExternalApiCallPolicy
import com.brifo.server.externalapi.ExternalApiCallService
import com.brifo.server.externalapi.idempotency.ExternalApiIdempotencyKeyGenerator
import com.brifo.server.externalapi.naver.NaverNewsProperties
import com.brifo.server.externalapi.naver.NaverNewsSearchResponse
import com.brifo.server.news.StockNewsKeywordPolicy
import com.brifo.server.news.entity.NewsSource
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import tools.jackson.databind.ObjectMapper
import java.net.URI
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Component
class NaverNewsCollectionClient(
    @Qualifier("naverNewsRestClient") private val restClient: RestClient,
    private val externalApiCallService: ExternalApiCallService,
    private val properties: NaverNewsProperties,
    private val objectMapper: ObjectMapper,
) : NewsCollectionClient {
    override fun collect(request: NewsCollectionClient.Request): NewsCollectionClient.Result {
        if (request.stocks.isEmpty()) {
            return NewsCollectionClient.Result(emptyList())
        }

        val news = request.stocks.asSequence().flatMap { stock ->
            val searchKeyword = StockNewsKeywordPolicy.searchKeyword(stock.name)
            val response = externalApiCallService.execute(
                provider = "NAVER", apiName = "NEWS_COLLECTION", policy = ExternalApiCallPolicy.NEWS_COLLECTION,
                retryEnabled = true,
                context = ExternalApiCallContext(
                    idempotencyKey = ExternalApiIdempotencyKeyGenerator.newsCollection(stock.code, request.targetDate),
                ),
                requestPayload = mapOf("stockCode" to stock.code, "query" to searchKeyword),
            ) {
                val rawResponse = restClient.get().uri {
                    it.path("/search/v1/news")
                        .queryParam("query", searchKeyword)
                        .queryParam("display", properties.searchDisplayCount)
                        .queryParam("sort", "date")
                        .queryParam("format", "json")
                        .build()
                }.retrieve().toEntity(String::class.java)
                // 네이버 API가 JSON 본문을 text/plain으로 내려줘서 메시지 컨버터가 자동 역직렬화를 못 한다.
                val body = checkNotNull(rawResponse.body) { "네이버 뉴스 응답 본문이 없습니다." }
                ResponseEntity.status(rawResponse.statusCode)
                    .body(objectMapper.readValue(body, NaverNewsSearchResponse::class.java))
            }
            response.items.asSequence().map { item ->
                val sourceUrl = item.link
                NewsCollectionClient.CollectedNews(
                    stockCode = stock.code,
                    source = NewsSource.NAVER,
                    sourceUrl = sourceUrl,
                    title = sanitizeText(item.title),
                    summary = sanitizeText(item.description),
                    dedupKey = "naver-news:${normalizeAndHash(sourceUrl)}",
                    publishedAt = parsePublishedAt(item.pubDate),
                )
            }
                // 제목 판정은 `<b>` 태그와 HTML 엔티티를 걷어낸 뒤에 해야 한다.
                .filter { StockNewsKeywordPolicy.isRelevant(stock.name, it.title) }
                .take(properties.collectCount)
        }.toList()
        return NewsCollectionClient.Result(news)
    }

    private fun sanitizeText(text: String): String {
        return text
            .replace(Regex("</?b>"), "")
            .replace("&quot;", "\"")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&#39;", "'")
            .replace("&amp;", "&")
    }

    private fun parsePublishedAt(pubDate: String): LocalDateTime {
        return ZonedDateTime.parse(pubDate, DateTimeFormatter.RFC_1123_DATE_TIME)
            .withZoneSameInstant(ZoneId.of("Asia/Seoul"))
            .toLocalDateTime()
    }

    private fun normalizeAndHash(url: String): String {
        val uri = URI(url)
        val normalized = "${uri.scheme?.lowercase()}://${uri.host?.lowercase()}${uri.path.removeSuffix("/")}"
        val digest = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
