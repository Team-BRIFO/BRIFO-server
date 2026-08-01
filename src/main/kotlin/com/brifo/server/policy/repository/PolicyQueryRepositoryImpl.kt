package com.brifo.server.policy.repository

import com.brifo.server.policy.dto.response.GetPendingPoliciesResponse
import com.brifo.server.policy.dto.response.GetPoliciesResponse
import com.brifo.server.policy.entity.QPolicy.Companion.policy
import com.brifo.server.policy.entity.QUserPolicy.Companion.userPolicy
import com.querydsl.core.types.Projections
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory

class PolicyQueryRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : PolicyQueryRepository {
    override fun findAllActiveWithAgreement(userId: Long): List<GetPoliciesResponse.PolicyItem> =
        queryFactory
            .select(
                Projections.constructor(
                    GetPoliciesResponse.PolicyItem::class.java,
                    policy.publicId,
                    policy.title,
                    policy.isRequired,
                    userPolicy.id.isNotNull,
                ),
            ).from(policy)
            .leftJoin(userPolicy)
            .on(
                userPolicy.policy.eq(policy),
                userPolicy.user.id.eq(userId),
                userPolicy.revokedAt.isNull,
            ).where(policy.isActive.isTrue)
            .orderBy(policy.publicId.asc())
            .fetch()

    override fun findPendingRequired(userId: Long): List<GetPendingPoliciesResponse.PendingPolicyItem> =
        queryFactory
            .select(
                Projections.constructor(
                    GetPendingPoliciesResponse.PendingPolicyItem::class.java,
                    policy.publicId,
                    policy.title,
                    policy.isRequired,
                    policy.version,
                ),
            ).from(policy)
            .where(
                policy.isActive.isTrue,
                policy.isRequired.isTrue,
                JPAExpressions
                    .selectOne()
                    .from(userPolicy)
                    .where(
                        userPolicy.user.id.eq(userId),
                        userPolicy.policy.eq(policy),
                        userPolicy.revokedAt.isNull,
                    ).notExists(),
            ).orderBy(policy.publicId.asc())
            .fetch()
}
