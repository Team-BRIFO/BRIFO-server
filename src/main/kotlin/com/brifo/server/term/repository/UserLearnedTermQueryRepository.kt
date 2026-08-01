package com.brifo.server.term.repository

import com.brifo.server.term.dto.response.GetMyTermsResponse
import java.util.UUID

interface UserLearnedTermQueryRepository {
    fun findPageByUserId(
        userId: Long,
        cursor: UUID?,
        limit: Int,
    ): List<GetMyTermsResponse.LearnedTermItem>
}
