package com.brifo.server.term.dto.response

import com.brifo.server.global.common.CursorPage
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.LocalDateTime
import java.util.UUID

data class GetMyTermsResponse(
    val learnedTermCount: Int,
    val page: CursorPage<LearnedTermItem>,
) {
    data class LearnedTermItem(
        @field:JsonIgnore
        val learnedTermId: UUID,
        val termId: UUID,
        val term: String,
        val definition: String,
        val category: String,
        @field:JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss[.SSSSSS]")
        val learnedAt: LocalDateTime,
    )
}
