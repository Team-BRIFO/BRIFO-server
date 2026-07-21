package com.brifo.server.stock.service

import com.brifo.server.stock.entity.Stock
import com.brifo.server.stock.entity.UserStock
import com.brifo.server.stock.exception.DuplicatedStockSelectionException
import com.brifo.server.stock.exception.StockNotFoundException
import com.brifo.server.stock.exception.StockSelectionMaximumExceededException
import com.brifo.server.stock.exception.StockSelectionMinimumNotMetException
import com.brifo.server.stock.repository.StockRepository
import com.brifo.server.stock.repository.UserStockRepository
import com.brifo.server.user.entity.User
import com.brifo.server.user.exception.UserNotFoundException
import com.brifo.server.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class StockService(
    private val userRepository: UserRepository,
    private val stockRepository: StockRepository,
    private val userStockRepository: UserStockRepository,
) {
    @Transactional
    fun updateInterests(
        userPublicId: UUID,
        stockIds: List<UUID>,
    ) {
        validateStockIds(stockIds)

        val user =
            userRepository.findByPublicId(userPublicId)
                ?: throw UserNotFoundException()

        val selectedStocks = findActiveStocks(stockIds)
        val existingUserStocks =
            userStockRepository.findAllByUserId(user.id!!)

        replaceInterests(
            user = user,
            selectedStocks = selectedStocks,
            existingUserStocks = existingUserStocks,
        )
    }

    private fun validateStockIds(stockIds: List<UUID>) {
        when {
            stockIds.isEmpty() ->
                throw StockSelectionMinimumNotMetException()

            stockIds.size > MAX_STOCK_COUNT ->
                throw StockSelectionMaximumExceededException()

            stockIds.size != stockIds.distinct().size ->
                throw DuplicatedStockSelectionException()
        }
    }

    private fun findActiveStocks(stockIds: List<UUID>): List<Stock> =
        stockRepository
            .findAllByPublicIdInAndIsActiveTrue(stockIds)
            .takeIf { it.size == stockIds.size }
            ?: throw StockNotFoundException()

    private fun replaceInterests(
        user: User,
        selectedStocks: List<Stock>,
        existingUserStocks: List<UserStock>,
    ) {
        val selectedStockIds =
            selectedStocks.mapNotNull { it.id }.toSet()

        val existingStockIds =
            existingUserStocks.mapNotNull { it.stock.id }.toSet()

        userStockRepository.deleteAll(
            existingUserStocks.filter {
                it.stock.id !in selectedStockIds
            },
        )

        userStockRepository.saveAll(
            selectedStocks
                .filter { it.id !in existingStockIds }
                .map { UserStock.create(user, it) },
        )
    }

    companion object {
        private const val MAX_STOCK_COUNT = 3
    }
}
