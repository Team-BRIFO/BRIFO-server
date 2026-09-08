package com.brifo.server.batch.generation

import com.brifo.server.TestcontainersConfiguration
import com.brifo.server.batch.common.BatchJobParameters
import com.brifo.server.news.entity.News
import com.brifo.server.news.entity.NewsSource
import com.brifo.server.news.repository.NewsCardRepository
import com.brifo.server.news.repository.NewsRepository
import com.brifo.server.stock.entity.Stock
import com.brifo.server.stock.repository.StockRepository
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.launch.JobOperator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Import(TestcontainersConfiguration::class)
@ActiveProfiles("test")
@SpringBootTest(
    properties = [
        "app.batch.scheduling-enabled=false",
        "external.data-server.base-url=http://localhost",
        // 생성 후보는 "관심종목 ∪ 기본 워치리스트"로 좁혀져 있다.
        // 테스트 종목이 어느 쪽에도 없으면 후보가 0건이라 카드가 만들어지지 않는다.
        "app.batch.default-watchlist-codes=AIT001",
    ],
)
class NewsCardGenerationJobAiMockIntegrationTest @Autowired constructor(
    private val jobOperator: JobOperator,
    @Qualifier(NewsCardGenerationJobConfiguration.JOB_NAME)
    private val job: Job,
    private val stockRepository: StockRepository,
    private val newsRepository: NewsRepository,
    private val newsCardRepository: NewsCardRepository,
) {
    @Test
    fun `AI 응답으로 카드뉴스를 저장하고 뉴스 원본 이미지를 복사한다`() {
        val targetDate = LocalDate.of(2026, 8, 3)
        val stock = stockRepository.save(Stock.create("AIT001", "AI테스트", "테스트"))
        val news = newsRepository.save(
            News.create(
                stock = stock,
                source = NewsSource.TEST,
                sourceUrl = "brifo",
                sourceImageUrl = SOURCE_IMAGE_URL,
                title = "AI 테스트 뉴스",
                summary = "AI 요청에 전달할 뉴스 요약",
                importance = BigDecimal("0.90"),
                dedupKey = "ai-mock-batch-test",
                publishedAt = LocalDateTime.of(targetDate, java.time.LocalTime.of(9, 0)),
            ),
        )
        val newsPublicId = requireNotNull(news.publicId)
        aiServer.enqueue(
            MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .setBody(
                    """{"isSuccess":true,"code":"COMMON200","message":"성공","result":{"newsId":"$newsPublicId","cardNews":[{"headline":"생성된 헤드라인","points":["첫 번째 포인트"],"keywords":["키워드"],"terms":[]}]}}""",
                ),
        )

        val execution = jobOperator.start(job, BatchJobParameters.forDevDate(targetDate))

        assertEquals(BatchStatus.COMPLETED, execution.status)
        val card = newsCardRepository.findAll().single { it.news.id == news.id }
        assertEquals(SOURCE_IMAGE_URL, card.imageUrl)
        assertEquals("생성된 헤드라인", card.headline)
        assertTrue(requireNotNull(newsRepository.findById(news.id!!).orElseThrow()).processingStatus.name == "PROCESSED")

        val request = aiServer.takeRequest()
        assertEquals("/ai/news/summarize", request.path)
        assertEquals("test-ai-key", request.getHeader("AI_INTERNAL_API_KEY"))
        assertEquals(null, request.getHeader(HttpHeaders.AUTHORIZATION))
        val requestBody = request.body.readUtf8()
        assertTrue(requestBody.contains("AI 요청에 전달할 뉴스 요약"))
        assertFalse(requestBody.contains("imageUrl"))
        assertFalse(requestBody.contains("sourceImageUrl"))
    }

    companion object {
        private const val SOURCE_IMAGE_URL = "https://cdn.example.com/news/source.webp"
        private val aiServer = MockWebServer().apply { start() }

        @JvmStatic
        @DynamicPropertySource
        fun aiProperties(registry: DynamicPropertyRegistry) {
            registry.add("external.ai.base-url") { aiServer.url("/").toString().removeSuffix("/") }
            registry.add("external.ai.api-key") { "test-ai-key" }
        }

        @JvmStatic
        @AfterAll
        fun stopServer() {
            aiServer.shutdown()
        }
    }
}
