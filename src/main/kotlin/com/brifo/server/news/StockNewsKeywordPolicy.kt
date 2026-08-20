package com.brifo.server.news

/**
 * 종목 뉴스 검색어와 관련성 판정 규칙.
 *
 * 네이버 뉴스 검색은 종목명이 본문 어딘가에 스치기만 해도 결과에 넣는다. 실제로 NAVER 종목에
 * "[포토뉴스]코레일, AI 활용 사이버공격 대응 훈련 실시" 같은 무관한 기사가 수집돼 카드뉴스로
 * 만들어졌다. **제목에 종목 표기가 없는 기사는 그 종목의 뉴스로 보지 않는다.**
 *
 * 본문(`description`)까지 보면 스쳐 지나가는 언급이 다시 통과하므로 제목만 본다.
 */
object StockNewsKeywordPolicy {
    /**
     * `stocks.name` 그대로 검색하면 결과가 엉뚱해지는 종목의 검색어 대체값.
     *
     * `NAVER`로 검색하면 국내 기사가 잘 안 잡히고 영문·무관 IT 기사가 섞인다.
     * 국내 기사는 `네이버`로 쓴다.
     */
    private val SEARCH_KEYWORDS: Map<String, String> = mapOf(
        "NAVER" to "네이버",
    )

    /**
     * 기사 제목에서 같은 종목으로 인정할 표기.
     *
     * `stocks.name`과 다르게 쓰이는 표기만 넣는다. 다른 회사까지 걸릴 만큼 넓은 표기는 넣지 않는다.
     * (예: `POSCO홀딩스`에 `포스코`를 넣으면 포스코인터내셔널 기사가 함께 걸린다.)
     */
    private val TITLE_ALIASES: Map<String, List<String>> = mapOf(
        "NAVER" to listOf("네이버"),
        "SK하이닉스" to listOf("하이닉스"),
        "현대차" to listOf("현대자동차"),
        "기아" to listOf("기아자동차"),
        "LG에너지솔루션" to listOf("LG엔솔"),
    )

    /** 네이버 뉴스 검색에 보낼 검색어. */
    fun searchKeyword(stockName: String): String = SEARCH_KEYWORDS[stockName] ?: stockName

    /** 기사 제목에 종목 표기가 있는지. */
    fun isRelevant(
        stockName: String,
        title: String,
    ): Boolean {
        val normalizedTitle = normalize(title)
        return titleKeywords(stockName).any { normalizedTitle.contains(it) }
    }

    private fun titleKeywords(stockName: String): List<String> =
        (listOf(stockName) + TITLE_ALIASES[stockName].orEmpty())
            .map(::normalize)
            .filter { it.isNotBlank() }

    /** 표기 흔들림(대소문자, `SK 하이닉스` 같은 띄어쓰기)을 흡수한다. */
    private fun normalize(value: String): String = value.lowercase().filterNot { it.isWhitespace() }
}
