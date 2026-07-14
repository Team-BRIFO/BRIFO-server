package com.brifo.server.term.dto.response

import com.brifo.server.global.common.CursorPage
import java.time.LocalDateTime
import java.util.UUID

data class GetMyTermsResponse(
    val learnedTermCount: Int,
    val page: CursorPage<Item>,
) {
    data class Item(
        val termId: UUID,
        val term: String,
        val definition: String,
        val category: String,
        val learnedAt: LocalDateTime,
    )
}
