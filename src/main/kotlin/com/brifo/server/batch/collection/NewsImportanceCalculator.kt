package com.brifo.server.batch.collection

import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode

@Component
class NewsImportanceCalculator {
    fun calculate(
        uniqueKeywordCount: Int,
        round: CollectionRound,
    ): BigDecimal {
        val keywordScore =
            BigDecimal(uniqueKeywordCount.coerceIn(0, MAX_KEYWORD_COUNT))
                .divide(BigDecimal(MAX_KEYWORD_COUNT), 4, RoundingMode.HALF_UP)

        return keywordScore
            .multiply(KEYWORD_WEIGHT)
            .add(round.recencyScore.multiply(RECENCY_WEIGHT))
            .setScale(2, RoundingMode.HALF_UP)
    }

    private companion object {
        const val MAX_KEYWORD_COUNT = 3
        val KEYWORD_WEIGHT = BigDecimal("0.7")
        val RECENCY_WEIGHT = BigDecimal("0.3")
    }
}
