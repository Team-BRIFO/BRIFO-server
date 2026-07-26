package com.brifo.server.news.controller

import com.brifo.server.ServerTestConfiguration
import com.brifo.server.news.entity.ImportanceBadge
import com.brifo.server.news.entity.News
import com.brifo.server.news.entity.NewsCard
import com.brifo.server.news.entity.NewsSource
import com.brifo.server.stock.entity.DailyStockPrice
import com.brifo.server.stock.entity.Stock
import com.brifo.server.term.entity.GlossaryTerm
import com.brifo.server.term.entity.NewsCardTerm
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Import(ServerTestConfiguration::class)
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class NewsControllerIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val entityManager: EntityManager,
) {
    @Test
    fun `카드뉴스 상세를 조회한다`() {
        // 테스트 DB에 조회할 카드뉴스 데이터를 저장한다.
        val (cardId, stockId) = saveNewsCardData()

        // 저장한 카드뉴스를 API로 조회하고 응답을 확인한다.
        mockMvc
            .perform(
                get("/api/news/{cardId}", cardId)
                    .param("userId", UUID.randomUUID().toString()),
            )
            // 요청이 성공했는지 확인한다.
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("COMMON_200"))
            // 연결된 종목이 반환되는지 확인한다.
            .andExpect(jsonPath("$.result.stock.stockId").value(stockId.toString()))
            // 뉴스 발행일 이전의 최신 가격이 선택됐는지 확인한다.
            .andExpect(jsonPath("$.result.stock.tradeDate").value("2026-07-03"))
            // 등락률이 소수점 한 자리로 반올림됐는지 확인한다.
            .andExpect(jsonPath("$.result.stock.changeRate").value(2.1))
            // 카드뉴스가 배열로 반환되는지 확인한다.
            .andExpect(jsonPath("$.result.newsCard").isArray)
            // 용어가 displayOrder 순서로 정렬됐는지 확인한다.
            .andExpect(jsonPath("$.result.newsCard[0].terms[0].displayOrder").value(0))
            .andExpect(jsonPath("$.result.newsCard[0].terms[1].displayOrder").value(1))
            // surface가 없으면 기본 용어가 사용되는지 확인한다.
            .andExpect(jsonPath("$.result.newsCard[0].terms[1].surface").value("순매수"))
    }

    @Test
    fun `잘못된 요청은 오류를 반환한다`() {
        val userId = UUID.randomUUID().toString()

        // cardId가 UUID 형식이 아니면 400을 반환해야 한다.
        mockMvc
            .perform(get("/api/news/not-a-uuid").param("userId", userId))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("COMMON_400"))

        // DB에 없는 cardId라면 404를 반환해야 한다.
        mockMvc
            .perform(get("/api/news/{cardId}", UUID.randomUUID()).param("userId", userId))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("CARD_404"))
    }

    private fun saveNewsCardData(): Pair<UUID, UUID> {
        // 테스트용 종목을 저장한다.
        val stock = Stock.create(code = "005930", name = "삼성전자", sector = "반도체")
        entityManager.persist(stock)

        // 위 종목에 연결된 원본 뉴스를 저장한다.
        val news =
            News.create(
                stock = stock,
                source = NewsSource.NAVER,
                sourceUrl = "https://example.com/news",
                title = "삼성전자 실적 개선",
                summary = null,
                importance = BigDecimal("0.90"),
                dedupKey = "news-card-test",
                publishedAt = LocalDateTime.of(2026, 7, 4, 10, 0),
            )
        entityManager.persist(news)

        // 위 뉴스에 연결된 카드뉴스를 저장한다.
        val newsCard =
            NewsCard.create(
                news = news,
                headline = "삼성전자 실적 개선",
                points = listOf("메모리 수요 회복"),
                keywords = listOf("HBM3E"),
                importanceBadge = ImportanceBadge.HOT,
                displayDate = LocalDate.of(2026, 7, 4),
            )
        entityManager.persist(newsCard)

        // 발행일 전후의 가격을 저장해 조회 기준을 확인한다.
        val prices =
            listOf(
                DailyStockPrice.create(stock, LocalDate.of(2026, 7, 1), BigDecimal("78000"), BigDecimal("1.0")),
                DailyStockPrice.create(stock, LocalDate.of(2026, 7, 3), BigDecimal("79200"), BigDecimal("2.14")),
                DailyStockPrice.create(stock, LocalDate.of(2026, 7, 5), BigDecimal("80000"), BigDecimal("3.0")),
            )

        prices.forEach { price ->
            entityManager.persist(price)
        }

        // 카드뉴스에 연결할 용어를 저장한다.
        val firstTerm = GlossaryTerm.create("목표주가", "목표로 제시한 주가", "투자")
        val secondTerm = GlossaryTerm.create("순매수", "매수가 많은 상태", "수급")
        entityManager.persist(firstTerm)
        entityManager.persist(secondTerm)

        // 두 번째로 보여줄 용어는 surface 없이 연결해 glossary_terms의 기본 용어를 사용하게 한다.
        val secondCardTerm =
            NewsCardTerm.create(
                newsCard = newsCard,
                term = secondTerm,
                surface = null,
                displayOrder = 1,
            )

        // 첫 번째로 보여줄 용어는 surface와 함께 연결해 해당 surface를 그대로 사용하게 한다.
        val firstCardTerm =
            NewsCardTerm.create(
                newsCard = newsCard,
                term = firstTerm,
                surface = "목표주가",
                displayOrder = 0,
            )

        // 용어가 저장 순서와 상관없이 화면 순서대로 나오는지 확인한다.
        entityManager.persist(secondCardTerm)
        entityManager.persist(firstCardTerm)

        // 저장한 데이터를 DB에 바로 반영한다.
        entityManager.flush()

        // API 요청에 사용할 카드 ID와 결과 확인에 사용할 종목 ID를 반환한다.
        return requireNotNull(newsCard.publicId) to requireNotNull(stock.publicId)
    }
}
