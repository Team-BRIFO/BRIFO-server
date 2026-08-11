package com.brifo.server.batch.settlement

import com.brifo.server.stock.entity.UserStock
import com.brifo.server.stock.repository.PendingUserStockRepository
import com.brifo.server.stock.repository.UserStockRepository
import com.brifo.server.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class PendingUserStockApplicationService(
    private val userRepository: UserRepository,
    private val userStockRepository: UserStockRepository,
    private val pendingUserStockRepository: PendingUserStockRepository,
) {
    @Transactional(readOnly = true)
    fun findTargetUserIds(effectiveAt: LocalDateTime): List<Long> =
        pendingUserStockRepository.findDistinctUserIdsByEffectiveAtLessThanEqual(effectiveAt)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun applyUser(
        userId: Long,
        effectiveAt: LocalDateTime,
    ) {
        val user = userRepository.findForUpdateById(userId) ?: return
        val pendingStocks = pendingUserStockRepository.findAllByUserAndEffectiveAtLessThanEqual(user, effectiveAt)
        if (pendingStocks.isEmpty()) return

        userStockRepository.deleteAllByUser(user)
        userStockRepository.saveAll(pendingStocks.map { UserStock.create(user, it.stock) })
        pendingUserStockRepository.deleteAllByUserAndEffectiveAtLessThanEqual(user, effectiveAt)
    }
}
