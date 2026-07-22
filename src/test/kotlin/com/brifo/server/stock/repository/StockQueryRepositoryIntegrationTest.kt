package com.brifo.server.stock.repository

import com.brifo.server.TestcontainersConfiguration
import com.brifo.server.global.config.JpaConfig
import com.brifo.server.global.config.QueryDslConfig
import com.brifo.server.stock.entity.DailyStockPrice
import com.brifo.server.stock.entity.Stock
import com.brifo.server.stock.entity.UserStock
import com.brifo.server.user.entity.OAuthProvider
import com.brifo.server.user.entity.User
import com.brifo.server.user.repository.UserRepository
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
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var userStockRepository: UserStockRepository

    @Autowired
    private lateinit var dailyStockPriceRepository: DailyStockPriceRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @Test
    fun `인기 종목은 선택 수와 종목 코드 순으로 활성 종목 최대 5개를 조회한다`() {
        val samsung = saveStock("005930", "삼성전자")
        val hynix = saveStock("000660", "SK하이닉스")
        val naver = saveStock("035420", "NAVER")

        // 선택 수가 0인 종목도 인기 종목 후보에 포함한다.
        val zeroFirst = saveStock("000001", "선택없음1")
        val zeroSecond = saveStock("000002", "선택없음2")
        val zeroThird = saveStock("000003", "선택없음3")

        val inactive = saveStock("999999", "비활성종목")
        val users = (1..3).map { saveUser("user-$it") }

        // 삼성전자 3회, SK하이닉스 2회, NAVER 1회 선택 상태를 만든다.
        select(users[0], samsung)
        select(users[1], samsung)
        select(users[2], samsung)
        select(users[0], hynix)
        select(users[1], hynix)
        select(users[0], naver)

        // 선택 수가 많아도 비활성 종목은 조회 대상에서 제외한다.
        users.forEach { select(it, inactive) }
        deactivate(inactive)

        // 같은 종목에 여러 가격이 있으면 가장 최근 거래일의 가격을 사용한다.
        savePrice(
            stock = samsung,
            price = "70000.00",
            changeRate = "1.00",
            tradeDate = LocalDate.of(2026, 7, 20),
        )
        savePrice(
            stock = samsung,
            price = "71000.00",
            changeRate = "2.14",
            tradeDate = LocalDate.of(2026, 7, 21),
        )

        entityManager.flush()
        entityManager.clear()

        val result = stockRepository.findPopularStocks()

        // 선택 수가 같으면 종목 코드 오름차순으로 정렬하고 최대 5개만 반환한다.
        assertEquals(5, result.size)
        assertEquals(
            listOf(
                samsung.publicId,
                hynix.publicId,
                naver.publicId,
                zeroFirst.publicId,
                zeroSecond.publicId,
            ),
            result.map { it.stockId },
        )
        assertTrue(result.none { it.stockId == inactive.publicId })
        assertTrue(result.none { it.stockId == zeroThird.publicId })

        val samsungResult = result.first()
        assertEquals(BigDecimal("71000.00"), samsungResult.price)
        assertEquals(BigDecimal("2.14"), samsungResult.changeRate)

        // 저장된 가격이 없는 종목은 가격 필드를 null로 반환한다.
        val hynixResult = result[1]
        assertNull(hynixResult.price)
        assertNull(hynixResult.changeRate)
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

    private fun saveUser(socialId: String): User =
        userRepository.saveAndFlush(
            User.create(
                provider = OAuthProvider.KAKAO,
                socialId = socialId,
                email = "$socialId@example.com",
            ),
        )

    private fun select(
        user: User,
        stock: Stock,
    ) {
        userStockRepository.save(
            UserStock.create(
                user = user,
                stock = stock,
            ),
        )
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
        // Entity에 비활성화 메서드가 없어 테스트 데이터만 native query로 변경한다.
        entityManager
            .createNativeQuery(
                "UPDATE stocks SET is_active = false WHERE id = :stockId",
            ).setParameter("stockId", requireNotNull(stock.id))
            .executeUpdate()
    }
}
