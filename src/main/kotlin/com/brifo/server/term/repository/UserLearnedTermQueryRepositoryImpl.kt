package com.brifo.server.term.repository

import com.brifo.server.term.dto.response.GetMyTermsResponse
import com.brifo.server.term.entity.QUserLearnedTerm.Companion.userLearnedTerm
import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import java.util.UUID

class UserLearnedTermQueryRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : UserLearnedTermQueryRepository {
    override fun findPageByUserId(
        userId: Long,
        cursor: UUID?,
        limit: Int,
    ): List<GetMyTermsResponse.Item> =
        queryFactory
            .select(
                Projections.constructor(
                    GetMyTermsResponse.Item::class.java,
                    userLearnedTerm.publicId,
                    userLearnedTerm.term.publicId,
                    userLearnedTerm.term.term,
                    userLearnedTerm.term.definition,
                    userLearnedTerm.term.category,
                    userLearnedTerm.learnedAt,
                ),
            ).from(userLearnedTerm)
            .join(userLearnedTerm.term)
            .where(
                userLearnedTerm.user.id.eq(userId),
                cursor?.let { userLearnedTerm.publicId.lt(it) },
            ).orderBy(userLearnedTerm.publicId.desc())
            .limit(limit.toLong())
            .fetch()
}
