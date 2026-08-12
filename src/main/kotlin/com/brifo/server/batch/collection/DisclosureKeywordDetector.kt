package com.brifo.server.batch.collection

import org.springframework.stereotype.Component

// 공시 여부를 별도 외부 API 없이, 수집한 뉴스 자체의 텍스트로 근사 판정한다.
@Component
class DisclosureKeywordDetector {
    fun matches(
        title: String,
        summary: String?,
    ): Boolean {
        val text = "$title ${summary.orEmpty()}"
        return KEYWORDS.any { text.contains(it) }
    }

    private companion object {
        val KEYWORDS = setOf("공시", "잠정실적", "실적발표", "분기보고서", "반기보고서", "사업보고서")
    }
}
