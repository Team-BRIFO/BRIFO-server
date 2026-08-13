package com.brifo.server.auth.dev

import jakarta.persistence.EntityManager
import jakarta.persistence.Query
import org.junit.jupiter.api.Test
import org.mockito.Answers
import org.mockito.Mockito.mock
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DevInitialApBalanceUpdaterTest {
    @Test
    fun `dev 사용자와 최초 지급 거래의 AP를 한 쿼리로 변경한다`() {
        val fixture = fixture(updatedTransactionCount = 1)

        fixture.updater.update("dev:social-id", 15)

        assertTrue(fixture.sql.contains("UPDATE users"))
        assertTrue(fixture.sql.contains("UPDATE ap_transactions"))
        assertEquals("dev:social-id", fixture.parameters["socialId"])
        assertEquals(15, fixture.parameters["initialBalanceAp"])
        assertTrue(fixture.flushed)
        assertTrue(fixture.cleared)
    }

    @Test
    fun `dev 사용자 또는 최초 지급 거래가 없으면 변경을 실패한다`() {
        val fixture = fixture(updatedTransactionCount = 0)

        assertFailsWith<IllegalStateException> {
            fixture.updater.update("dev:missing", 15)
        }
    }

    private fun fixture(updatedTransactionCount: Int): Fixture {
        val parameters = mutableMapOf<String, Any>()
        lateinit var query: Query
        query =
            mock(Query::class.java) { invocation ->
                when (invocation.method.name) {
                    "setParameter" -> {
                        parameters[invocation.arguments[0] as String] = invocation.arguments[1]
                        query
                    }
                    "executeUpdate" -> updatedTransactionCount
                    else -> Answers.RETURNS_DEFAULTS.answer(invocation)
                }
            }

        var sql = ""
        var flushed = false
        var cleared = false
        val entityManager =
            mock(EntityManager::class.java) { invocation ->
                when (invocation.method.name) {
                    "flush" -> {
                        flushed = true
                        null
                    }
                    "clear" -> {
                        cleared = true
                        null
                    }
                    "createNativeQuery" -> {
                        sql = invocation.arguments[0] as String
                        query
                    }
                    else -> Answers.RETURNS_DEFAULTS.answer(invocation)
                }
            }

        return Fixture(
            updater = DevInitialApBalanceUpdater(entityManager),
            parameters = parameters,
            sqlProvider = { sql },
            flushedProvider = { flushed },
            clearedProvider = { cleared },
        )
    }

    private class Fixture(
        val updater: DevInitialApBalanceUpdater,
        val parameters: Map<String, Any>,
        private val sqlProvider: () -> String,
        private val flushedProvider: () -> Boolean,
        private val clearedProvider: () -> Boolean,
    ) {
        val sql: String get() = sqlProvider()
        val flushed: Boolean get() = flushedProvider()
        val cleared: Boolean get() = clearedProvider()
    }
}
