package com.brifo.server.batch.collection

import org.springframework.stereotype.Component
@Component
class NewsImportanceCalculator {
    fun calculate(round: CollectionRound) = round.recencyScore.setScale(2)
}
