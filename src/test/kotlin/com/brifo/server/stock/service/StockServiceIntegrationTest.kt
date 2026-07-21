package com.brifo.server.stock.service

import com.brifo.server.TestcontainersConfiguration
import com.brifo.server.global.config.JpaConfig
import com.brifo.server.global.config.QueryDslConfig
import com.brifo.server.stock.entity.Stock
import com.brifo.server.stock.entity.UserStock
import com.brifo.server.stock.exception.DuplicatedStockSelectionException
import com.brifo.server.stock.exception.StockNotFoundException
import com.brifo.server.stock.exception.StockSelectionMaximumExceededException
import com.brifo.server.stock.exception.StockSelectionMinimumNotMetException
import com.brifo.server.stock.repository.StockRepository
import com.brifo.server.stock.repository.UserStockRepository
import com.brifo.server.user.entity.OAuthProvider
import com.brifo.server.user.entity.User
import com.brifo.server.user.repository.UserRepository
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.util.UUID

@DataJpaTest
@Import(
    TestcontainersConfiguration::class,
    JpaConfig::class,
    QueryDslConfig::class,
    StockService::class,
)
@ActiveProfiles("test")
class StockServiceIntegrationTest {
    @Autowired
    private lateinit var stockService: StockService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var stockRepository: StockRepository

    @Autowired
    private lateinit var userStockRepository: UserStockRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @Test
    fun `관심 종목을 전체 교체하고 같은 요청을 반복해도 중복되지 않는다`() {
        val user = saveUser("user-1")
        val firstStock = saveStock("000001")
        val secondStock = saveStock("000002")
        val thirdStock = saveStock("000003")

        userStockRepository.saveAllAndFlush(
            listOf(
                UserStock.create(user, firstStock),
                UserStock.create(user, secondStock),
            ),
        )

        val requestedStockIds =
            listOf(
                secondStock.publicId!!,
                thirdStock.publicId!!,
            )

        stockService.updateInterests(user.publicId!!, requestedStockIds)

        assertEquals(
            setOf(secondStock.id, thirdStock.id),
            findInterestStockIds(user),
        )

        stockService.updateInterests(user.publicId!!, requestedStockIds)

        assertEquals(
            setOf(secondStock.id, thirdStock.id),
            findInterestStockIds(user),
        )
    }

    @Test
    fun `선택 개수가 1개 미만이거나 3개를 초과하면 실패한다`() {
        val user = saveUser("user-2")

        assertThrows<StockSelectionMinimumNotMetException> {
            stockService.updateInterests(user.publicId!!, emptyList())
        }

        assertThrows<StockSelectionMaximumExceededException> {
            stockService.updateInterests(
                user.publicId!!,
                List(4) { UUID.randomUUID() },
            )
        }
    }

    @Test
    fun `중복된 종목이 포함되면 실패한다`() {
        val user = saveUser("user-3")
        val stock = saveStock("000004")

        assertThrows<DuplicatedStockSelectionException> {
            stockService.updateInterests(
                user.publicId!!,
                listOf(stock.publicId!!, stock.publicId!!),
            )
        }
    }

    @Test
    fun `존재하지 않는 종목이 포함되면 기존 관심 종목을 유지한다`() {
        val user = saveUser("user-4")
        val existingStock = saveStock("000005")

        userStockRepository.saveAndFlush(
            UserStock.create(user, existingStock),
        )

        assertThrows<StockNotFoundException> {
            stockService.updateInterests(
                user.publicId!!,
                listOf(existingStock.publicId!!, UUID.randomUUID()),
            )
        }

        assertEquals(
            setOf(existingStock.id),
            findInterestStockIds(user),
        )
    }

    @Test
    fun `비활성 종목이 포함되면 기존 관심 종목을 유지한다`() {
        val user = saveUser("user-5")
        val existingStock = saveStock("000006")
        val inactiveStock = saveStock("000007")

        userStockRepository.saveAndFlush(
            UserStock.create(user, existingStock),
        )

        entityManager
            .createNativeQuery("UPDATE stocks SET is_active = false WHERE id = ?1")
            .setParameter(1, inactiveStock.id)
            .executeUpdate()
        entityManager.clear()

        assertThrows<StockNotFoundException> {
            stockService.updateInterests(
                user.publicId!!,
                listOf(existingStock.publicId!!, inactiveStock.publicId!!),
            )
        }

        assertEquals(
            setOf(existingStock.id),
            findInterestStockIds(user),
        )
    }

    private fun saveUser(socialId: String): User =
        userRepository.saveAndFlush(
            User.create(
                provider = OAuthProvider.KAKAO,
                socialId = socialId,
                email = "$socialId@test.com",
            ),
        )

    private fun saveStock(code: String): Stock =
        stockRepository.saveAndFlush(
            Stock.create(
                code = code,
                name = "테스트 종목",
                sector = "테스트",
            ),
        )

    private fun findInterestStockIds(user: User): Set<Long?> {
        entityManager.flush()
        entityManager.clear()

        return userStockRepository
            .findAllByUserId(user.id!!)
            .map { it.stock.id }
            .toSet()
    }
}
