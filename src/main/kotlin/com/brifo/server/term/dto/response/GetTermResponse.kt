package com.brifo.server.term.dto.response

import java.util.UUID

data class GetTermResponse(
    val termId: UUID,
    val term: String,
    val definition: String,
    val category: String,
    val isLearned: Boolean,
)
