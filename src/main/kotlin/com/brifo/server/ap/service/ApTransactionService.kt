package com.brifo.server.ap.service

import com.brifo.server.ap.entity.ApTransaction
import com.brifo.server.ap.entity.ApTransactionReason
import com.brifo.server.ap.entity.ApTransactionTargetType
import com.brifo.server.ap.exception.InsufficientApBalanceException
import com.brifo.server.ap.repository.ApTransactionRepository
import com.brifo.server.user.exception.UserNotFoundException
import com.brifo.server.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ApTransactionService(
    private val userRepository: UserRepository,
    private val apTransactionRepository: ApTransactionRepository,
) {
    data class Target(
        val type: ApTransactionTargetType,
        val id: Long,
    )

    @Transactional
    fun change(
        userId: UUID,
        deltaAp: Int,
        reason: ApTransactionReason,
        target: Target? = null,
    ): Int {
        val user = userRepository.findForUpdateByPublicId(userId) ?: throw UserNotFoundException()
        if (user.balanceAp + deltaAp < 0) {
            throw InsufficientApBalanceException()
        }
        val transaction =
            ApTransaction.create(
                user = user,
                amount = deltaAp,
                reason = reason,
                targetType = target?.type,
                targetId = target?.id,
            )

        user.changeAp(deltaAp)
        apTransactionRepository.save(transaction)
        return user.balanceAp
    }
}
