package com.brifo.server.auth.dev

import jakarta.persistence.EntityManager
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("dev")
@ConditionalOnProperty(prefix = "app.dev-auth", name = ["enabled"], havingValue = "true")
class DevInitialApBalanceUpdater(
    private val entityManager: EntityManager,
) {
    fun update(
        socialId: String,
        initialBalanceAp: Int,
    ) {
        entityManager.flush()
        val updatedTransactionCount =
            entityManager
                .createNativeQuery(
                    """
                    WITH target_user AS (
                        SELECT id
                        FROM users
                        WHERE provider = 'KAKAO'
                          AND social_id = :socialId
                          AND deleted_at IS NULL
                    ),
                    updated_user AS (
                        UPDATE users
                        SET balance_ap = :initialBalanceAp,
                            updated_at = NOW()
                        WHERE id = (SELECT id FROM target_user)
                        RETURNING id
                    )
                    UPDATE ap_transactions
                    SET amount = :initialBalanceAp
                    WHERE user_id = (SELECT id FROM updated_user)
                      AND reason = 'INITIAL_GRANT'
                    """.trimIndent(),
                ).setParameter("socialId", socialId)
                .setParameter("initialBalanceAp", initialBalanceAp)
                .executeUpdate()

        check(updatedTransactionCount == 1) {
            "Dev user and its initial AP grant must exist exactly once."
        }
        entityManager.clear()
    }
}
