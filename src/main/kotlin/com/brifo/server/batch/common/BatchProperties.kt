package com.brifo.server.batch.common

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("app.batch")
data class BatchProperties(
    val schedulingEnabled: Boolean = false,
    val restartDelay: Duration = Duration.ofMinutes(10),
    val maxExecutions: Int = 3,
    /**
     * 관심종목으로 담은 유저가 없어도 항상 뉴스를 수집·생성할 종목 코드.
     *
     * 수집 대상을 관심종목으로만 좁히면 `user_stocks`가 비었을 때 뉴스가 영원히 0건이 되고,
     * 신규 유저가 처음 담은 종목도 다음 배치까지 카드뉴스가 비어 있게 된다.
     */
    val defaultWatchlistCodes: List<String> = DEFAULT_WATCHLIST_CODES,
) {
    init {
        require(!restartDelay.isNegative && !restartDelay.isZero) { "restartDelay는 양수여야 합니다." }
        require(maxExecutions >= 1) { "maxExecutions는 1 이상이어야 합니다." }
        require(defaultWatchlistCodes.isNotEmpty()) { "defaultWatchlistCodes는 최소 1개여야 합니다." }
    }

    private companion object {
        // 시가총액 상위 10종목. R__stocks.sql 시드에 포함된 코드여야 한다.
        val DEFAULT_WATCHLIST_CODES =
            listOf(
                "005930", // 삼성전자
                "000660", // SK하이닉스
                "373220", // LG에너지솔루션
                "207940", // 삼성바이오로직스
                "005380", // 현대차
                "000270", // 기아
                "068270", // 셀트리온
                "005490", // POSCO홀딩스
                "035420", // NAVER
                "035720", // 카카오
            )
    }
}
