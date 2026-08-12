package com.brifo.server.batch.collection

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DisclosureKeywordDetectorTest {
    private val detector = DisclosureKeywordDetector()

    @Test
    fun `제목에 공시 키워드가 있으면 true다`() {
        assertTrue(detector.matches("브리포테크, 정기공시 통해 실적 공개", "요약"))
    }

    @Test
    fun `요약에만 공시 키워드가 있어도 true다`() {
        assertTrue(detector.matches("브리포테크 소식", "이번 분기보고서에 따르면..."))
    }

    @Test
    fun `공시 키워드가 없으면 false다`() {
        assertFalse(detector.matches("브리포테크, 신제품 출시", "신규 라인업을 공개했다"))
    }

    @Test
    fun `요약이 없어도 예외 없이 판정한다`() {
        assertFalse(detector.matches("브리포테크, 신제품 출시", null))
        assertTrue(detector.matches("브리포테크, 잠정실적 공개", null))
    }
}
