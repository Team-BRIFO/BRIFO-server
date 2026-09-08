package com.brifo.server.news.client

import com.brifo.server.externalapi.ExternalApiCallService
import com.brifo.server.externalapi.log.service.ExternalApiCallLogService
import com.brifo.server.externalapi.naver.NaverNewsProperties
import com.brifo.server.news.entity.NewsSource
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NaverNewsCollectionClientTest {
    private val callService = ExternalApiCallService(mock(ExternalApiCallLogService::class.java))
    private val objectMapper = jacksonObjectMapper()
    private val properties = NaverNewsProperties(searchDisplayCount = 20, collectCount = 3)
    private val stock = NewsCollectionClient.StockRef("BRIFO01", "브리포테크")
    private val request = NewsCollectionClient.Request(
        targetDate = LocalDate.of(2026, 8, 12),
        stocks = listOf(stock),
    )

    @Test
    fun `종목이 없으면 네이버 API를 호출하지 않는다`() {
        val fixture = restClient()
        val client = NaverNewsCollectionClient(fixture.client, callService, properties, objectMapper)

        val result = client.collect(request.copy(stocks = emptyList()))

        assertTrue(result.news.isEmpty())
        fixture.server.verify()
    }

    @Test
    fun `네이버 뉴스 응답을 도메인 필드로 매핑한다`() {
        val fixture = restClient()
        val client = NaverNewsCollectionClient(fixture.client, callService, properties, objectMapper)
        fixture.server.expect(requestTo(containsString("/search/v1/news")))
            .andExpect(method(HttpMethod.GET))
            .andExpect(queryParam("query", java.net.URLEncoder.encode("브리포테크", "UTF-8")))
            .andExpect(queryParam("display", "20"))
            .andExpect(queryParam("sort", "date"))
            .andRespond(withSuccess(newsResponse(), MediaType.APPLICATION_JSON))

        val result = client.collect(request).news.single()

        assertEquals("BRIFO01", result.stockCode)
        assertEquals(NewsSource.NAVER, result.source)
        assertEquals("https://n.news.naver.com/mnews/article/001/0000000001?query=abc", result.sourceUrl)
        assertEquals("브리포테크 실적 발표", result.title)
        assertEquals("""브리포테크가 실적을 "발표"했다.""", result.summary)
        assertEquals(LocalDateTime.of(2026, 8, 12, 9, 0, 0), result.publishedAt)
        assertTrue(result.dedupKey.matches(Regex("naver-news:[0-9a-f]{64}")))
        fixture.server.verify()
    }

    @Test
    fun `쿼리스트링만 다른 동일 원문 링크는 같은 dedupKey로 정규화된다`() {
        val fixtureA = restClient()
        val clientA = NaverNewsCollectionClient(fixtureA.client, callService, properties, objectMapper)
        fixtureA.server.expect(requestTo(containsString("/search/v1/news")))
            .andRespond(
                withSuccess(
                    newsResponse(link = "https://n.news.naver.com/mnews/article/001/0000000001?query=abc"),
                    MediaType.APPLICATION_JSON,
                ),
            )
        val dedupKeyA = clientA.collect(request).news.single().dedupKey

        val fixtureB = restClient()
        val clientB = NaverNewsCollectionClient(fixtureB.client, callService, properties, objectMapper)
        fixtureB.server.expect(requestTo(containsString("/search/v1/news")))
            .andRespond(
                withSuccess(
                    newsResponse(link = "https://n.news.naver.com/mnews/article/001/0000000001?query=xyz&ref=home"),
                    MediaType.APPLICATION_JSON,
                ),
            )
        val dedupKeyB = clientB.collect(request).news.single().dedupKey

        assertEquals(dedupKeyA, dedupKeyB)
    }

    @Test
    fun `제목에 종목 표기가 없는 기사는 수집하지 않는다`() {
        val fixture = restClient()
        val client = NaverNewsCollectionClient(fixture.client, callService, properties, objectMapper)
        fixture.server.expect(requestTo(containsString("/search/v1/news")))
            .andRespond(
                withSuccess(
                    itemsResponse(
                        item(title = "[포토뉴스]코레일, AI 활용 사이버공격 대응 훈련 실시", link = "https://n.example/1"),
                        item(title = "<b>브리포테크</b> 실적 발표", link = "https://n.example/2"),
                    ),
                    MediaType.APPLICATION_JSON,
                ),
            )

        val result = client.collect(request).news

        assertEquals(listOf("브리포테크 실적 발표"), result.map { it.title })
        fixture.server.verify()
    }

    @Test
    fun `관련 기사가 많아도 수집 건수 상한까지만 가져온다`() {
        val fixture = restClient()
        val client = NaverNewsCollectionClient(fixture.client, callService, properties, objectMapper)
        fixture.server.expect(requestTo(containsString("/search/v1/news")))
            .andRespond(
                withSuccess(
                    itemsResponse(
                        *(1..5).map { index ->
                            item(title = "브리포테크 소식 $index", link = "https://n.example/$index")
                        }.toTypedArray(),
                    ),
                    MediaType.APPLICATION_JSON,
                ),
            )

        val result = client.collect(request).news

        assertEquals(3, result.size)
        assertEquals(listOf("브리포테크 소식 1", "브리포테크 소식 2", "브리포테크 소식 3"), result.map { it.title })
        fixture.server.verify()
    }

    @Test
    fun `종목명이 기사 표기와 다르면 대체 검색어로 조회한다`() {
        val fixture = restClient()
        val client = NaverNewsCollectionClient(fixture.client, callService, properties, objectMapper)
        fixture.server.expect(requestTo(containsString("/search/v1/news")))
            .andExpect(queryParam("query", java.net.URLEncoder.encode("네이버", "UTF-8")))
            .andRespond(
                withSuccess(
                    itemsResponse(item(title = "네이버, 2분기 실적 발표", link = "https://n.example/1")),
                    MediaType.APPLICATION_JSON,
                ),
            )

        val result = client.collect(
            request.copy(stocks = listOf(NewsCollectionClient.StockRef("035420", "NAVER"))),
        ).news

        assertEquals(listOf("네이버, 2분기 실적 발표"), result.map { it.title })
        fixture.server.verify()
    }

    private fun restClient(): RestFixture {
        val builder = RestClient.builder()
            .baseUrl("http://naver-test")
            .defaultHeader("X-NCP-APIGW-API-KEY-ID", "test-client-id")
            .defaultHeader("X-NCP-APIGW-API-KEY", "test-client-secret")
        val server = MockRestServiceServer.bindTo(builder).build()
        return RestFixture(builder.build(), server)
    }

    private fun newsResponse(link: String = "https://n.news.naver.com/mnews/article/001/0000000001?query=abc") =
        """
        {
          "total": 1,
          "start": 1,
          "display": 20,
          "items": [
            {
              "title": "<b>브리포테크</b> 실적 발표",
              "originallink": "https://press.example/1",
              "link": "$link",
              "description": "브리포테크가 실적을 &quot;발표&quot;했다.",
              "pubDate": "Wed, 12 Aug 2026 09:00:00 +0900"
            }
          ]
        }
        """.trimIndent()

    private fun item(
        title: String,
        link: String,
    ) = """
        {
          "title": "$title",
          "originallink": "https://press.example/1",
          "link": "$link",
          "description": "본문 요약",
          "pubDate": "Wed, 12 Aug 2026 09:00:00 +0900"
        }
    """.trimIndent()

    private fun itemsResponse(vararg items: String) =
        """
        {
          "total": ${items.size},
          "start": 1,
          "display": 20,
          "items": [${items.joinToString(",")}]
        }
        """.trimIndent()

    private data class RestFixture(val client: RestClient, val server: MockRestServiceServer)
}
