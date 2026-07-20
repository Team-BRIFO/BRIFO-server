package com.brifo.server.stock.repository

import com.brifo.server.stock.entity.QUserStock.Companion.userStock
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class UserStockQueryRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : UserStockQueryRepository {
    override fun existsInterestStock(
        userPublicId: UUID,
        stockPublicId: UUID,
    ): Boolean =
        queryFactory
            .selectOne()
            .from(userStock)
            .where(
                userStock.user.publicId.eq(userPublicId),
                userStock.stock.publicId.eq(stockPublicId),
            ).fetchFirst() != null
}
