package com.brifo.server.term.service

import com.brifo.server.badge.code.BadgeCode
import com.brifo.server.badge.service.BadgeAwardService
import com.brifo.server.global.common.CursorPage
import com.brifo.server.term.dto.request.GetMyTermsRequest
import com.brifo.server.term.dto.response.GetMyTermsResponse
import com.brifo.server.term.dto.response.GetTermResponse
import com.brifo.server.term.exception.TermNotFoundException
import com.brifo.server.term.repository.GlossaryTermRepository
import com.brifo.server.term.repository.UserLearnedTermRepository
import com.brifo.server.user.exception.UserNotFoundException
import com.brifo.server.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class TermService(
    private val glossaryTermRepository: GlossaryTermRepository,
    private val userLearnedTermRepository: UserLearnedTermRepository,
    private val userRepository: UserRepository,
    private val badgeAwardService: BadgeAwardService,
) {
    @Transactional(readOnly = true)
    fun getMyTerms(
        userPublicId: UUID,
        request: GetMyTermsRequest,
    ): GetMyTermsResponse {
        val userId = findUserId(userPublicId)
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
        userPublicId: UUID,
        termId: UUID,
    ): GetTermResponse {
        val userId = findUserId(userPublicId)
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
        userPublicId: UUID,
        termId: UUID,
    ) {
        val userId = findUserId(userPublicId)
        val term = glossaryTermRepository.findByPublicId(termId) ?: throw TermNotFoundException()

        userLearnedTermRepository.insertIfAbsent(
            userId = userId,
            termId = requireNotNull(term.id),
        )

        if (userLearnedTermRepository.countByUserId(userId) >= TERM_LEARNING_BADGE_COUNT) {
            badgeAwardService.awardBadge(userPublicId, BadgeCode.B11)
        }
    }

    private fun findUserId(userPublicId: UUID): Long =
        userRepository.findByPublicId(userPublicId)?.id ?: throw UserNotFoundException()

    private companion object {
        const val TERM_LEARNING_BADGE_COUNT = 10
    }
}
