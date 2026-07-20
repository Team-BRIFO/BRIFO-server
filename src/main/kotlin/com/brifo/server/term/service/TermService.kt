package com.brifo.server.term.service

import com.brifo.server.global.common.CursorPage
import com.brifo.server.term.dto.request.GetMyTermsRequest
import com.brifo.server.term.dto.response.GetMyTermsResponse
import com.brifo.server.term.dto.response.GetTermResponse
import com.brifo.server.term.exception.TermNotFoundException
import com.brifo.server.term.repository.GlossaryTermRepository
import com.brifo.server.term.repository.UserLearnedTermRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class TermService(
    private val glossaryTermRepository: GlossaryTermRepository,
    private val userLearnedTermRepository: UserLearnedTermRepository,
) {
    @Transactional(readOnly = true)
    fun getMyTerms(
        userId: Long,
        request: GetMyTermsRequest,
    ): GetMyTermsResponse {
        val learnedTerms =
            userLearnedTermRepository.findPageByUserId(
                userId = userId,
                cursor = request.cursor,
                limit = request.size + 1,
            )
        val hasNext = learnedTerms.size > request.size
        val pageItems = learnedTerms.take(request.size)

        return GetMyTermsResponse(
            learnedTermCount = Math.toIntExact(userLearnedTermRepository.countByUserId(userId)),
            page =
                CursorPage(
                    items = pageItems,
                    nextCursor =
                        if (hasNext) {
                            pageItems.last().learnedTermId
                        } else {
                            null
                        },
                    hasNext = hasNext,
                ),
        )
    }

    @Transactional(readOnly = true)
    fun getTerm(
        userId: Long,
        termId: UUID,
    ): GetTermResponse {
        val term = glossaryTermRepository.findByPublicId(termId) ?: throw TermNotFoundException()
        val internalTermId = requireNotNull(term.id)
        val isLearned = userLearnedTermRepository.existsByUserIdAndTermId(userId, internalTermId)

        return GetTermResponse(
            termId = requireNotNull(term.publicId),
            term = term.term,
            definition = term.definition,
            category = term.category,
            isLearned = isLearned,
        )
    }

    @Transactional
    fun saveTerm(
        userId: Long,
        termId: UUID,
    ) {
        val term = glossaryTermRepository.findByPublicId(termId) ?: throw TermNotFoundException()

        userLearnedTermRepository.insertIfAbsent(
            userId = userId,
            termId = requireNotNull(term.id),
        )
    }
}
