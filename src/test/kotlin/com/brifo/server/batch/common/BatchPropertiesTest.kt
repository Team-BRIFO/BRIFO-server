package com.brifo.server.batch.common

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BatchPropertiesTest {
    @Test
    fun `기본 워치리스트가 비어 있으면 기동에 실패한다`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                BatchProperties(defaultWatchlistCodes = emptyList())
            }

        assertEquals("defaultWatchlistCodes는 최소 1개여야 합니다.", exception.message)
    }

    @Test
    fun `기본 워치리스트에 빈 종목 코드가 있으면 기동에 실패한다`() {
        // IN ('') 은 어떤 종목도 잡지 못해 조용히 수집·생성이 멈춘다.
        listOf(listOf(""), listOf("   "), listOf("005930", "")).forEach { codes ->
            val exception =
                assertFailsWith<IllegalArgumentException>("codes=$codes 는 거부되어야 한다.") {
                    BatchProperties(defaultWatchlistCodes = codes)
                }

            assertEquals("defaultWatchlistCodes에는 빈 종목 코드를 넣을 수 없습니다.", exception.message)
        }
    }

    @Test
    fun `기본값은 유효한 종목 코드만 가진다`() {
        val properties = BatchProperties()

        assertTrue(properties.defaultWatchlistCodes.isNotEmpty())
        assertTrue(properties.defaultWatchlistCodes.none { it.isBlank() })
    }
}
