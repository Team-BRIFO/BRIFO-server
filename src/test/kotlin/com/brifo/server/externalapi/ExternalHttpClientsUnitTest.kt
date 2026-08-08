package com.brifo.server.externalapi

import com.brifo.server.briefing.client.AiBriefingAnalysisClient
import com.brifo.server.briefing.client.BriefingAnalysisClient
import com.brifo.server.externalapi.log.service.ExternalApiCallLogService
import com.brifo.server.news.client.AiNewsCardGenerationClient
import com.brifo.server.news.client.DataServerDisclosureClient
import com.brifo.server.news.client.DataServerNewsCollectionClient
import com.brifo.server.news.client.DisclosureClient
import com.brifo.server.news.client.NewsCardGenerationClient
import com.brifo.server.news.client.NewsCollectionClient
import com.brifo.server.stock.client.ClosingPriceClient
import com.brifo.server.stock.client.CurrentStockPriceClient
import com.brifo.server.stock.client.DataServerStockPriceClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class ExternalHttpClientsUnitTest {
    private val callService = ExternalApiCallService(mock(ExternalApiCallLogService::class.java))

    @Test
    fun `데이터 서버 현재가와 종가 응답을 도메인 결과로 변환한다`() {
        val fixture = restClient()
        val client = DataServerStockPriceClient(
            fixture.client,
            callService,
            Clock.fixed(Instant.parse("2026-08-09T03:00:00Z"), ZoneId.of("Asia/Seoul")),
        )
        fixture.server.expect(requestTo("http://data-server/api/stocks/BRIFO01/prices?date=2026-08-09"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(priceResponse("2026-08-09"), MediaType.APPLICATION_JSON))
        fixture.server.expect(requestTo("http://data-server/api/stocks/BRIFO01/prices?date=2026-08-08"))
            .andRespond(withSuccess(priceResponse("2026-08-08"), MediaType.APPLICATION_JSON))

        val current = client.getCurrentPrice(CurrentStockPriceClient.Request(1L, "BRIFO01"))
        val closing = client.getClosingPrice(ClosingPriceClient.Request("BRIFO01", LocalDate.of(2026, 8, 8)))

        assertEquals(BigDecimal("31850.00"), current.currentPrice)
        assertNull(current.priceChange)
        assertEquals(BigDecimal("17.96"), current.changeRate)
        assertEquals(BigDecimal("31850.00"), closing.price)
        fixture.server.verify()
    }

    @Test
    fun `데이터 서버 뉴스의 URL과 이미지를 매핑한다`() {
        val fixture = restClient()
        val client = DataServerNewsCollectionClient(fixture.client, callService)
        fixture.server.expect(requestTo("http://data-server/api/stocks/BRIFO01/news?date=2026-08-09"))
            .andRespond(withSuccess(newsResponse(), MediaType.APPLICATION_JSON))

        val result = client.collect(
            NewsCollectionClient.Request(
                LocalDate.of(2026, 8, 9),
                LocalDate.of(2026, 8, 9).atTime(11, 30),
                listOf("BRIFO01"),
            ),
        ).news.single()

        assertEquals("https://data.example/news/1", result.sourceUrl)
        assertEquals("https://cdn.example/news/1.webp", result.sourceImageUrl)
        assertEquals("data-server:0198d73d-5df0-7000-8000-000000000001", result.dedupKey)
        fixture.server.verify()
    }

    @Test
    fun `공시 없음은 정상적인 false 결과로 반환한다`() {
        val fixture = restClient()
        val client = DataServerDisclosureClient(fixture.client, callService)
        fixture.server.expect(requestTo("http://data-server/api/stocks/BRIFO01/disclosures/exists?date=2026-08-09"))
            .andRespond(withSuccess(disclosureResponse(), MediaType.APPLICATION_JSON))

        val exists = client.exists(DisclosureClient.Request(stockCode = "BRIFO01", date = LocalDate.of(2026, 8, 9)))

        assertFalse(exists)
        fixture.server.verify()
    }

    @Test
    fun `카드뉴스 AI 계약과 Bearer 인증을 사용한다`() {
        val fixture = restClient()
        val client = AiNewsCardGenerationClient(fixture.client, callService)
        fixture.server.expect(requestTo("http://data-server/ai/news/summarize"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-key"))
            .andRespond(withSuccess(cardResponse(), MediaType.APPLICATION_JSON))

        val result = client.createNewsCard(
            NewsCardGenerationClient.Request(
                UUID.fromString("0198d73d-5df0-7000-8000-000000000001"),
                "브리포테크",
                "뉴스 본문",
                listOf("PER"),
            ),
        )

        assertEquals("헤드라인", result.cardNews.single().headline)
        fixture.server.verify()
    }

    @Test
    fun `브리핑 AI의 논리적 실패는 예외로 처리한다`() {
        val fixture = restClient()
        val client = AiBriefingAnalysisClient(fixture.client, callService)
        fixture.server.expect(requestTo("http://data-server/ai/briefing/generate"))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-key"))
            .andRespond(withSuccess("""{"isSuccess":false,"code":"BRIEFING502","message":"분석 실패","result":null}""", MediaType.APPLICATION_JSON))

        val exception = assertThrows(IllegalStateException::class.java) {
            client.createBriefings(BriefingAnalysisClient.Request(UUID.randomUUID(), emptyList(), emptyList(), "1-1", emptyList()))
        }

        assertTrue(exception.message.orEmpty().contains("BRIEFING502"))
        fixture.server.verify()
    }

    private fun restClient(): RestFixture {
        val builder = RestClient.builder()
            .baseUrl("http://data-server")
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer test-key")
        val server = MockRestServiceServer.bindTo(builder).build()
        return RestFixture(builder.build(), server)
    }

    private fun priceResponse(date: String) =
        """{"success":true,"code":"COMMON_200","message":"성공","result":{"stock":{"name":"브리포테크","sector":"브리포","code":"BRIFO01"},"stockPrice":{"price":31850.00,"changeRate":17.96,"tradeDate":"$date"}}}"""

    private fun newsResponse() =
        """{"success":true,"code":"COMMON_200","message":"성공","result":{"stock":{"name":"브리포테크","sector":"브리포","code":"BRIFO01"},"news":[{"newsId":"0198d73d-5df0-7000-8000-000000000001","title":"뉴스","content":"본문","sourceUrl":"https://data.example/news/1","imageUrl":"https://cdn.example/news/1.webp","publishedAt":"2026-08-09T09:00:00"}]}}"""

    private fun disclosureResponse() =
        """{"success":true,"code":"COMMON_200","message":"성공","result":{"stock":{"name":"브리포테크","sector":"브리포","code":"BRIFO01"},"disclosure":{"hasDisclosure":false}}}"""

    private fun cardResponse() =
        """{"isSuccess":true,"code":"COMMON200","message":"성공","result":{"newsId":"0198d73d-5df0-7000-8000-000000000001","cardNews":[{"headline":"헤드라인","points":["포인트"],"keywords":["키워드"],"terms":[]}]}}"""

    private data class RestFixture(val client: RestClient, val server: MockRestServiceServer)
}
