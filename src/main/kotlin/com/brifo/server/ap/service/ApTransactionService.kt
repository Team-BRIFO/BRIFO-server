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
    data class ChangeResult(
        val deltaAp: Int,
        val balanceAp: Int,
    )

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
    ): Int = changeLocked(userId, deltaAp, reason, target, floorAtZero = false).balanceAp

    @Transactional
    fun settleDecision(
        userId: UUID,
        plannedDeltaAp: Int,
        reason: ApTransactionReason,
        decisionId: Long,
    ): ChangeResult {
        require(reason in DECISION_REASONS) { "Invalid decision AP reason: $reason" }
        return changeLocked(
            userId = userId,
            requestedDeltaAp = plannedDeltaAp,
            reason = reason,
            target = Target(ApTransactionTargetType.DECISION, decisionId),
            floorAtZero = true,
        )
    }

    private fun changeLocked(
        userId: UUID,
        requestedDeltaAp: Int,
        reason: ApTransactionReason,
        target: Target?,
        floorAtZero: Boolean,
    ): ChangeResult {
        val user = userRepository.findForUpdateByPublicId(userId) ?: throw UserNotFoundException()
        if (!floorAtZero && user.balanceAp + requestedDeltaAp < 0) {
            throw InsufficientApBalanceException()
        }
        val actualDelta = if (floorAtZero) requestedDeltaAp.coerceAtLeast(-user.balanceAp) else requestedDeltaAp
        val transaction =
            ApTransaction.create(
                user = user,
                amount = actualDelta,
                reason = reason,
                targetType = target?.type,
                targetId = target?.id,
            )
        user.changeAp(actualDelta)
        apTransactionRepository.save(transaction)
        return ChangeResult(actualDelta, user.balanceAp)
    }

    private companion object {
        val DECISION_REASONS =
            setOf(
                ApTransactionReason.DECISION_WIN,
                ApTransactionReason.DECISION_LOSE,
                ApTransactionReason.NEUTRAL_HIT,
            )
    }
}
