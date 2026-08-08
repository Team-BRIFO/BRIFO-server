package com.brifo.server.term.repository

import com.brifo.server.news.entity.QNewsCard.Companion.newsCard
import com.brifo.server.term.entity.QGlossaryTerm.Companion.glossaryTerm
import com.brifo.server.term.entity.QNewsCardTerm.Companion.newsCardTerm
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class NewsCardTermQueryRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : NewsCardTermQueryRepository {
    override fun findTermsUsedBetween(
        fromInclusive: LocalDate,
        toExclusive: LocalDate,
    ): List<String> =
        queryFactory
            .select(glossaryTerm.term)
            .distinct()
            .from(newsCardTerm)
            .join(newsCardTerm.newsCard, newsCard)
            .join(newsCardTerm.term, glossaryTerm)
            .where(
                newsCard.displayDate.goe(fromInclusive),
                newsCard.displayDate.lt(toExclusive),
            ).orderBy(glossaryTerm.term.asc())
            .fetch()
}
