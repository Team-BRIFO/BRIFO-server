package com.brifo.server.user.service

import com.brifo.server.stock.exception.DuplicatedStockSelectionException
import com.brifo.server.stock.exception.StockSelectionMaximumExceededException
import com.brifo.server.stock.exception.StockSelectionMinimumNotMetException
import com.brifo.server.user.entity.User
import com.brifo.server.user.exception.InvalidCompanyNameException
import com.brifo.server.user.exception.InvalidNicknameException
import com.brifo.server.user.exception.OnboardingAlreadyCompletedException
import com.brifo.server.user.exception.OnboardingProfileNotCompletedException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class UserValidationService {
    fun normalizeNickname(nickname: String): String =
        nickname.trim().takeIf { it.length in NICKNAME_LENGTH_RANGE }
            ?: throw InvalidNicknameException()

    fun normalizeCompanyName(companyName: String?): String? {
        if (companyName == null) return null

        return companyName.trim().takeIf { it.length in COMPANY_NAME_LENGTH_RANGE }
            ?: throw InvalidCompanyNameException()
    }

    fun validateStockIds(stockIds: List<UUID>) {
        when {
            stockIds.isEmpty() -> throw StockSelectionMinimumNotMetException()
            stockIds.size > MAX_STOCK_SELECTION_COUNT -> throw StockSelectionMaximumExceededException()
            stockIds.distinct().size != stockIds.size -> throw DuplicatedStockSelectionException()
        }
    }

    fun requireOnboardingPending(user: User) {
        if (user.onboardingCompletedAt != null) {
            throw OnboardingAlreadyCompletedException()
        }
    }

    fun requireOnboardingCompleted(user: User) {
        if (user.onboardingCompletedAt == null) {
            throw OnboardingProfileNotCompletedException()
        }
    }

    companion object {
        private val NICKNAME_LENGTH_RANGE = 1..50
        private val COMPANY_NAME_LENGTH_RANGE = 1..100
        private const val MAX_STOCK_SELECTION_COUNT = 3
    }
}
