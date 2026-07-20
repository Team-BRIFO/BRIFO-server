package com.brifo.server.ap.repository

interface ApTransactionQueryRepository {
    fun sumBriefingSalaryBalance(briefingId: Long): Int
}
