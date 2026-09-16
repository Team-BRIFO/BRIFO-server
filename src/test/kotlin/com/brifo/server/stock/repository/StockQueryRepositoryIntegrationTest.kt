package com.brifo.server.stock.repository

import com.brifo.server.TestcontainersConfiguration
import com.brifo.server.global.config.JpaConfig
import com.brifo.server.global.config.QueryDslConfig
import com.brifo.server.stock.entity.DailyStockPrice
import com.brifo.server.stock.entity.Stock
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.LocalDate

@DataJpaTest
@Import(
    TestcontainersConfiguration::class,
    JpaConfig::class,
    QueryDslConfig::class,
)
@ActiveProfiles("test")
class StockQueryRepositoryIntegrationTest {
    @Autowired
    private lateinit var stockRepository: StockRepository

    @Autowired
    private lateinit var dailyStockPriceRepository: DailyStockPriceRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @Test
    fun `인기 종목은 fame_rank, 최근 종가, 종목 코드 순으로 활성 종목을 limit 만큼 조회한다`() {
        // fame_rank가 없는(NULL) 종목들 — 최근 종가 내림차순, 동률이면 코드 오름차순으로 정렬돼야 한다.
        val noRankHigh = saveStock("000001", "무랭크고가")
        val noRankLow = saveStock("000002", "무랭크저가")
        val noRankNoPrice = saveStock("000003", "무랭크가격없음")

        // fame_rank가 있으면 가격과 무관하게 그 순서를 우선한다.
        val hynix = saveStockWithFameRank("000660", "SK하이닉스", fameRank = 1)
        val samsung = saveStockWithFameRank("005930", "삼성전자", fameRank = 2)

        val inactive = saveStockWithFameRank("999999", "비활성종목", fameRank = 0)
        deactivate(inactive)

        savePrice(stock = noRankHigh, price = "90000.00", changeRate = "1.00", tradeDate = LocalDate.of(2026, 7, 21))
        savePrice(stock = noRankLow, price = "10000.00", changeRate = "1.00", tradeDate = LocalDate.of(2026, 7, 21))
        // 같은 종목에 여러 가격이 있으면 가장 최근 거래일의 가격을 사용한다.
        savePrice(stock = samsung, price = "70000.00", changeRate = "1.00", tradeDate = LocalDate.of(2026, 7, 20))
        savePrice(stock = samsung, price = "71000.00", changeRate = "2.14", tradeDate = LocalDate.of(2026, 7, 21))

        entityManager.flush()
        entityManager.clear()

        val result = stockRepository.findPopularStocks(limit = 10)

        // fame_rank가 있는 종목이 먼저, 그 다음 fame_rank가 없는 종목은 최근 종가 내림차순(가격 없는 종목은 0으로 취급해 맨 뒤)이다.
        assertEquals(
            listOf(hynix.publicId, samsung.publicId, noRankHigh.publicId, noRankLow.publicId, noRankNoPrice.publicId),
            result.map { it.stockId },
        )
        assertTrue(result.none { it.stockId == inactive.publicId })

        val samsungResult = result.first { it.stockId == samsung.publicId }
        assertEquals(BigDecimal("71000.00"), samsungResult.price)
        assertEquals(BigDecimal("2.14"), samsungResult.changeRate)

        // 저장된 가격이 없는 종목도 화면 필수 필드를 0으로 반환한다.
        val hynixResult = result.first { it.stockId == hynix.publicId }
        assertEquals(BigDecimal.ZERO, hynixResult.price)
        assertEquals(BigDecimal.ZERO, hynixResult.changeRate)
    }

    @Test
    fun `인기 종목은 limit 개수만큼만 조회한다`() {
        (1..7).forEach { index -> saveStock(index.toString().padStart(6, '0'), "종목$index") }

        entityManager.flush()
        entityManager.clear()

        val result = stockRepository.findPopularStocks(limit = 3)

        assertEquals(3, result.size)
    }

    @Test
    fun `검색은 공백과 영문 대소문자를 무시하고 비활성 종목을 제외한다`() {
        val samsung = saveStock("005930", "삼성전자")
        val english = saveStock("111111", "Samsung Electronics")
        val inactive = saveStock("222222", "Samsung Test")

        deactivate(inactive)

        entityManager.flush()
        entityManager.clear()

        // 검색어와 종목명 양쪽의 공백을 제거해 부분 일치 검색한다.
        val koreanResult =
            stockRepository.searchStocks(
                keyword = "삼성 전자",
                cursor = null,
                limit = 20,
            )

        // 영문 종목명은 대소문자를 구분하지 않고 검색한다.
        val englishResult =
            stockRepository.searchStocks(
                keyword = "sAmSuNg eLeCtRoNiCs",
                cursor = null,
                limit = 20,
            )

        assertEquals(listOf(samsung.publicId), koreanResult.map { it.stockId })
        assertEquals(listOf(english.publicId), englishResult.map { it.stockId })
        assertTrue(englishResult.none { it.stockId == inactive.publicId })
    }

    @Test
    fun `검색 커서는 UUID 내림차순에서 커서 다음 종목부터 조회한다`() {
        val stocks =
            (1..4).map { index ->
                saveStock(
                    code = index.toString().padStart(6, '0'),
                    name = "테스트종목$index",
                )
            }

        entityManager.flush()
        entityManager.clear()

        // DB에서 생성된 UUID를 실제 검색 정렬 순서와 동일하게 정렬한다.
        val orderedStocks =
            stocks.sortedByDescending { requireNotNull(it.publicId) }
        val cursor = requireNotNull(orderedStocks.first().publicId)

        // 커서보다 작은 UUID 중 앞의 2개만 조회한다.
        val result =
            stockRepository.searchStocks(
                keyword = "테스트 종목",
                cursor = cursor,
                limit = 2,
            )

        assertEquals(
            orderedStocks
                .drop(1)
                .take(2)
                .map { it.publicId },
            result.map { it.stockId },
        )
    }

    private fun saveStock(
        code: String,
        name: String,
    ): Stock =
        stockRepository
            .saveAndFlush(
                Stock.create(
                    code = code,
                    name = name,
                    sector = "테스트",
                ),
            ).also {
                // DB에서 생성되는 publicId를 테스트 객체에 반영한다.
                entityManager.refresh(it)
            }

    private fun saveStockWithFameRank(
        code: String,
        name: String,
        fameRank: Int,
    ): Stock {
        val stock = saveStock(code, name)
        entityManager
            .createNativeQuery("UPDATE stocks SET fame_rank = ?1 WHERE id = ?2")
            .setParameter(1, fameRank)
            .setParameter(2, requireNotNull(stock.id))
            .executeUpdate()
        entityManager.clear()
        return stock
    }

    private fun savePrice(
        stock: Stock,
        price: String,
        changeRate: String,
        tradeDate: LocalDate,
    ) {
        dailyStockPriceRepository.save(
            DailyStockPrice.create(
                stock = stock,
                tradeDate = tradeDate,
                price = BigDecimal(price),
                changeRate = BigDecimal(changeRate),
            ),
        )
    }

    private fun deactivate(stock: Stock) {
        entityManager
            .createNativeQuery(
                "UPDATE stocks SET is_active = false WHERE id = ?1", // ?1 == 첫번째 매게변수 자리
            )
            .setParameter(1, requireNotNull(stock.id)) // 첫번째 매게변수 자리에, stock.id를 넣는다. (1번 행X, 하드코딩X)
            .executeUpdate()

        entityManager.clear()
    }
}
