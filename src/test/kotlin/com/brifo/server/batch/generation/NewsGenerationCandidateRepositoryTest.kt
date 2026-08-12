package com.brifo.server.batch.generation

import com.brifo.server.TestcontainersConfiguration
import com.brifo.server.global.config.JpaConfig
import com.brifo.server.global.config.QueryDslConfig
import com.brifo.server.news.entity.ImportanceBadge
import com.brifo.server.news.entity.News
import com.brifo.server.news.entity.NewsCard
import com.brifo.server.news.entity.NewsSource
import com.brifo.server.news.repository.NewsCardRepository
import com.brifo.server.news.repository.NewsRepository
import com.brifo.server.stock.entity.Stock
import com.brifo.server.stock.entity.UserStock
import com.brifo.server.stock.repository.StockRepository
import com.brifo.server.user.entity.OAuthProvider
import com.brifo.server.user.entity.User
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@DataJpaTest
@Import(TestcontainersConfiguration::class, JpaConfig::class, QueryDslConfig::class)
@ActiveProfiles("test")
class NewsGenerationCandidateRepositoryTest {
    @Autowired
    private lateinit var stockRepository: StockRepository

    @Autowired
    private lateinit var newsRepository: NewsRepository

    @Autowired
    private lateinit var cardRepository: NewsCardRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @Test
    fun `이미 카드가 있는 뉴스를 제외한 뒤 상위 2건을 선정한다`() {
        val stock = stockRepository.save(Stock.create("BRF001", "브리포주식", "금융"))
        val unassociatedStock = stockRepository.save(Stock.create("BRF002", "미관심주식", "금융"))
        val user = User.create(OAuthProvider.KAKAO, "batch-user", "batch-user@example.com")
        entityManager.persist(user)
        entityManager.persist(UserStock.create(user, stock))
        val first = saveNews(stock, "first", "0.90", LocalDateTime.of(2026, 8, 3, 11, 0))
        val second = saveNews(stock, "second", "0.80", LocalDateTime.of(2026, 8, 3, 10, 0))
        val third = saveNews(stock, "third", "0.70", LocalDateTime.of(2026, 8, 3, 9, 0))
        val unassociated = saveNews(unassociatedStock, "unassociated", "1.00", LocalDateTime.of(2026, 8, 3, 12, 0))
        cardRepository.save(
            NewsCard.create(
                news = first,
                headline = "첫 번째",
                points = listOf("포인트"),
                keywords = listOf("키워드"),
                importanceBadge = ImportanceBadge.HOT,
                displayDate = LocalDate.of(2026, 8, 4),
            ),
        )
        entityManager.flush()
        entityManager.clear()

        val candidates = newsRepository.findGenerationCandidateIds(
            LocalDate.of(2026, 8, 3).atStartOfDay(),
            LocalDate.of(2026, 8, 4).atStartOfDay(),
        )

        assertEquals(listOf(requireNotNull(second.id), requireNotNull(third.id)), candidates)
        assertFalse(requireNotNull(unassociated.id) in candidates)
    }

    private fun saveNews(
        stock: Stock,
        key: String,
        importance: String,
        publishedAt: LocalDateTime,
    ): News =
        newsRepository.save(
            News.create(
                stock = stock,
                source = NewsSource.TEST,
                sourceUrl = "https://example.com/$key",
                title = key,
                summary = key,
                importance = BigDecimal(importance),
                dedupKey = key,
                publishedAt = publishedAt,
            ),
        )
}
