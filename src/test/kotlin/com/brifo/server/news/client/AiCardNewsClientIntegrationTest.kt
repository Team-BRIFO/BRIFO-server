package com.brifo.server.news.client

import com.brifo.server.ServerTestConfiguration
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// 실제 AI 서버 주소와 인증키가 있을 때만 실행한다.
//@EnabledIfEnvironmentVariable(named = "AI_BASE_URL", matches = ".+")
//@EnabledIfEnvironmentVariable(named = "AI_INTERNAL_API_KEY", matches = ".+")
@Import(ServerTestConfiguration::class)
@ActiveProfiles("test")
@SpringBootTest(
    properties = [
        "external.ai.base-url=\${AI_BASE_URL:disabled}",
        "external.ai.api-key=\${AI_INTERNAL_API_KEY:disabled}",
    ],
)
class AiCardNewsClientIntegrationTest @Autowired constructor(
    private val newsSummaryClient: NewsSummaryClient,
) {
    @Test
    fun `AI 서버에서 카드뉴스 요약을 받아온다`() {
        // 실제 AI 서버로 보낼 뉴스 데이터다.
        val request =
            NewsSummaryClient.Request(
                newsId = "news_20250721_005930",
                stockName = "삼성전자",
                newsContent =
                    """
                    삼성전자가 3분기 잠정 실적을 발표하며 매출 79조원,
                    영업이익 10조원을 기록했다고 밝혔다.
                    이는 시장 컨센서스를 상회하는 수치로,
                    메모리 반도체 가격 상승과 AI 서버향 수요 증가가
                    실적 개선을 견인한 것으로 분석된다.
                    """.trimIndent(),
                excludeTerms = listOf("공매도", "PER"),
            )

        // 기존 AiCardNewsClient를 통해 실제 AI 서버를 호출한다.
        val response = newsSummaryClient.createCardNews(request)

        // AI 요청이 성공했는지 확인한다.
        assertTrue(response.isSuccess)
        assertEquals("COMMON200", response.code)

        // 응답에 요청한 뉴스 ID와 카드뉴스가 있는지 확인한다.
        val result = assertNotNull(response.result)
        assertEquals(request.newsId, result.newsId)
        assertTrue(result.cardNews.isNotEmpty())

        // 첫 번째 카드뉴스의 주요 내용이 비어 있지 않은지 확인한다.
        val cardNews = result.cardNews.first()
        assertTrue(cardNews.headline.isNotBlank())
        assertTrue(cardNews.points.isNotEmpty())
        assertTrue(cardNews.keywords.isNotEmpty())
    }
}
