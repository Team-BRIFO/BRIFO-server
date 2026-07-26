package com.brifo.server.badge.repository

import com.brifo.server.TestcontainersConfiguration
import com.brifo.server.global.config.JpaConfig
import com.brifo.server.global.config.QueryDslConfig
import com.brifo.server.user.entity.OAuthProvider
import com.brifo.server.user.entity.User
import com.brifo.server.user.repository.UserRepository
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@DataJpaTest
@Import(TestcontainersConfiguration::class, JpaConfig::class, QueryDslConfig::class)
@ActiveProfiles("test")
class BadgeRepositoryTest {
    @Autowired
    private lateinit var badgeRepository: BadgeRepository

    @Autowired
    private lateinit var userBadgeRepository: UserBadgeRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @Test
    fun `목록은 코드순으로 모든 뱃지와 사용자의 보유 여부를 반환한다`() {
        val user = saveUser("owner")
        val ownedBadge = requireNotNull(badgeRepository.findByCode("B02"))
        userBadgeRepository.insertIfAbsent(requireNotNull(user.publicId), requireNotNull(ownedBadge.id))
        entityManager.clear()

        val badges = badgeRepository.findAllWithOwnership(requireNotNull(user.publicId))

        assertEquals((1..12).map { "B${it.toString().padStart(2, '0')}" }, badges.map { it.code })
        assertTrue(badges.single { it.code == "B02" }.isOwned)
        assertTrue(badges.filterNot { it.code == "B02" }.none { it.isOwned })
    }

    @Test
    fun `상세 조회는 현재 사용자가 보유한 뱃지만 반환한다`() {
        val owner = saveUser("owner")
        val otherUser = saveUser("other")
        val badge = requireNotNull(badgeRepository.findByCode("B01"))
        userBadgeRepository.insertIfAbsent(requireNotNull(owner.publicId), requireNotNull(badge.id))
        entityManager.clear()

        val owned = badgeRepository.findOwnedBadge(requireNotNull(owner.publicId), requireNotNull(badge.publicId))
        val notOwned = badgeRepository.findOwnedBadge(requireNotNull(otherUser.publicId), requireNotNull(badge.publicId))

        requireNotNull(owned)
        assertEquals("B01", owned.code)
        assertEquals(50, owned.rewardAp)
        assertNull(notOwned)
    }

    @Test
    fun `동일한 뱃지 획득은 한 번만 저장된다`() {
        val user = saveUser("owner")
        val badge = requireNotNull(badgeRepository.findByCode("B01"))
        val userPublicId = requireNotNull(user.publicId)
        val badgeId = requireNotNull(badge.id)

        assertEquals(1, userBadgeRepository.insertIfAbsent(userPublicId, badgeId))
        assertEquals(0, userBadgeRepository.insertIfAbsent(userPublicId, badgeId))
        assertEquals(1L, userBadgeRepository.count())
    }

    private fun saveUser(socialId: String): User =
        userRepository.saveAndFlush(
            User.create(
                provider = OAuthProvider.KAKAO,
                socialId = socialId,
                email = "$socialId@example.com",
            ),
        ).also {
            entityManager.refresh(it)
        }
}
