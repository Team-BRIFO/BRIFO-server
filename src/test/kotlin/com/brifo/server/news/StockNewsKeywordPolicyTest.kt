package com.brifo.server.news

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StockNewsKeywordPolicyTest {
    @Test
    fun `종목명이 기사 표기와 같으면 그대로 검색한다`() {
        assertEquals("삼성전자", StockNewsKeywordPolicy.searchKeyword("삼성전자"))
    }

    @Test
    fun `국내 기사 표기가 다른 종목은 대체 검색어를 쓴다`() {
        assertEquals("네이버", StockNewsKeywordPolicy.searchKeyword("NAVER"))
    }

    @Test
    fun `제목에 종목 표기가 있으면 관련 기사로 본다`() {
        assertTrue(StockNewsKeywordPolicy.isRelevant("삼성전자", "삼성전자, 파운드리 가격 최대 15% 인상"))
    }

    @Test
    fun `제목에 종목 표기가 없으면 관련 기사가 아니다`() {
        // NAVER 종목에 실제로 수집됐던 기사. 본문에 스친 언급만으로 검색에 걸렸다.
        assertFalse(
            StockNewsKeywordPolicy.isRelevant("NAVER", "[포토뉴스]코레일, AI 활용 사이버공격 대응 훈련 실시"),
        )
    }

    @Test
    fun `종목명과 다른 표기로 쓴 제목도 관련 기사로 본다`() {
        assertTrue(StockNewsKeywordPolicy.isRelevant("NAVER", "네이버, 2분기 실적 발표"))
        assertTrue(StockNewsKeywordPolicy.isRelevant("NAVER", "NAVER Invests in Panthalassa"))
        assertTrue(StockNewsKeywordPolicy.isRelevant("현대차", "현대자동차, 미국 공장 증설"))
        assertTrue(StockNewsKeywordPolicy.isRelevant("SK하이닉스", "하이닉스 HBM 공급 확대"))
    }

    @Test
    fun `띄어쓰기와 대소문자가 달라도 같은 종목으로 본다`() {
        assertTrue(StockNewsKeywordPolicy.isRelevant("SK하이닉스", "SK 하이닉스, 신고가 경신"))
        assertTrue(StockNewsKeywordPolicy.isRelevant("NAVER", "naver, 신규 서비스 출시"))
    }
}
