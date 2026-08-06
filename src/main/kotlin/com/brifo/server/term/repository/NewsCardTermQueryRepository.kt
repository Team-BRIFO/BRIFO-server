package com.brifo.server.term.repository

import java.time.LocalDate

interface NewsCardTermQueryRepository {
    fun findTermsUsedBetween(
        fromInclusive: LocalDate,
        toExclusive: LocalDate,
    ): List<String>
}
