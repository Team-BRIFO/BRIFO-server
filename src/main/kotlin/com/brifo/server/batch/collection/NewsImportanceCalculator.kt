package com.brifo.server.batch.collection

import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class NewsImportanceCalculator {
    fun calculate(
        round: CollectionRound,
        hasDisclosure: Boolean,
    ): BigDecimal {
        val disclosureScore = if (hasDisclosure) BigDecimal.ONE else BigDecimal.ZERO

        return disclosureScore
            .multiply(DISCLOSURE_WEIGHT)
            .add(round.recencyScore.multiply(RECENCY_WEIGHT))
            .setScale(2)
    }

    private companion object {
        val DISCLOSURE_WEIGHT = BigDecimal("0.7")
        val RECENCY_WEIGHT = BigDecimal("0.3")
    }
}
