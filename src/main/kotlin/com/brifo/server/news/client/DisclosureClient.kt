package com.brifo.server.news.client

import java.time.LocalDate

interface DisclosureClient {
    fun exists(request: Request): Boolean
    data class Request(val stockId: Long? = null, val stockCode: String, val date: LocalDate)
}
