package com.brifo.server.batch.settlement

data class DecisionSettlementItem(
    val decisionId: Long,
    val dailyStockPriceId: Long,
    val isCorrect: Boolean,
)
