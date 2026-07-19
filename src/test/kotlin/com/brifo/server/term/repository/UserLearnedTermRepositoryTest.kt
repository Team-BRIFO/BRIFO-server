package com.brifo.server.term.repository

import com.brifo.server.TestcontainersConfiguration
import com.brifo.server.global.config.JpaConfig
import com.brifo.server.global.config.QueryDslConfig
import com.brifo.server.term.entity.GlossaryTerm
import com.brifo.server.term.entity.UserLearnedTerm
import com.brifo.server.user.entity.OAuthProvider
import com.brifo.server.user.entity.User
import com.brifo.server.user.repository.UserRepository
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@DataJpaTest
@Import(TestcontainersConfiguration::class, JpaConfig::class, QueryDslConfig::class)
@ActiveProfiles("test")
class UserLearnedTermRepositoryTest {
    @Autowired
    private lateinit var userLearnedTermRepository: UserLearnedTermRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var glossaryTermRepository: GlossaryTermRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @Test
    fun `멱등 INSERT는 동일 사용자와 용어를 한 번만 저장하고 learnedAt을 유지한다`() {
        val user = saveUser("user-1")
        val term = saveTerm("PER")

        assertEquals(
            1,
            userLearnedTermRepository.insertIfAbsent(requireNotNull(user.id), requireNotNull(term.id)),
        )
        entityManager.flush()
        entityManager.clear()
        val first = userLearnedTermRepository.findPageByUserId(requireNotNull(user.id), null, 10).single()
        val firstLearnedAt = first.learnedAt

        assertEquals(
            0,
            userLearnedTermRepository.insertIfAbsent(requireNotNull(user.id), requireNotNull(term.id)),
        )
        entityManager.flush()
        entityManager.clear()
        val saved = userLearnedTermRepository.findPageByUserId(requireNotNull(user.id), null, 10).single()

        assertEquals(firstLearnedAt, saved.learnedAt)
        assertEquals(1L, userLearnedTermRepository.countByUserId(requireNotNull(user.id)))
    }

    @Test
    fun `같은 용어도 사용자가 다르면 각각 저장된다`() {
        val firstUser = saveUser("user-1")
        val secondUser = saveUser("user-2")
        val term = saveTerm("PER")

        userLearnedTermRepository.insertIfAbsent(requireNotNull(firstUser.id), requireNotNull(term.id))
        userLearnedTermRepository.insertIfAbsent(requireNotNull(secondUser.id), requireNotNull(term.id))
        entityManager.clear()

        assertEquals(1L, userLearnedTermRepository.countByUserId(requireNotNull(firstUser.id)))
        assertEquals(1L, userLearnedTermRepository.countByUserId(requireNotNull(secondUser.id)))
    }

    @Test
    fun `커서 조회는 사용자 기록을 publicId 내림차순으로 제한 개수만 반환한다`() {
        val user = saveUser("user-1")
        val otherUser = saveUser("user-2")
        val terms = (1..4).map { saveTerm("TERM-$it") }
        val userRecords =
            terms.take(3).map { term ->
                userLearnedTermRepository.saveAndFlush(UserLearnedTerm.create(user, term)).also {
                    entityManager.refresh(it)
                }
        }
        userLearnedTermRepository.saveAndFlush(UserLearnedTerm.create(otherUser, terms[3]))
        val recordsByNewest = userRecords.sortedByDescending { requireNotNull(it.publicId) }
        val expectedAfterCursor = recordsByNewest[1]
        val expectedTermId = requireNotNull(expectedAfterCursor.term.publicId)
        val expectedTerm = expectedAfterCursor.term.term
        val expectedDefinition = expectedAfterCursor.term.definition
        val expectedLearnedAt = expectedAfterCursor.learnedAt
        entityManager.flush()
        entityManager.clear()

        val cursor = requireNotNull(recordsByNewest.first().publicId)
        val firstPage = userLearnedTermRepository.findPageByUserId(requireNotNull(user.id), null, 2)
        val afterCursor = userLearnedTermRepository.findPageByUserId(requireNotNull(user.id), cursor, 10)

        assertEquals(recordsByNewest.take(2).map { it.publicId }, firstPage.map { it.learnedTermId })
        assertEquals(recordsByNewest.drop(1).map { it.publicId }, afterCursor.map { it.learnedTermId })
        with(afterCursor.first()) {
            assertEquals(expectedTermId, termId)
            assertEquals(expectedTerm, term)
            assertEquals(expectedDefinition, definition)
            assertEquals("지표", category)
            assertEquals(expectedLearnedAt, learnedAt)
        }
    }

    private fun saveUser(socialId: String): User =
        userRepository.saveAndFlush(
            User.create(
                provider = OAuthProvider.KAKAO,
                socialId = socialId,
                email = "$socialId@example.com",
            ),
        )

    private fun saveTerm(term: String): GlossaryTerm =
        glossaryTermRepository.saveAndFlush(
            GlossaryTerm.create(
                term = term,
                definition = "$term definition",
                category = "지표",
            ),
        )
}
