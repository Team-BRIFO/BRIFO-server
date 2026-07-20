package com.brifo.server.stock.repository

import java.util.UUID

interface UserStockQueryRepository {
    fun existsInterestStock(
        userPublicId: UUID,
        stockPublicId: UUID,
    ): Boolean
}
