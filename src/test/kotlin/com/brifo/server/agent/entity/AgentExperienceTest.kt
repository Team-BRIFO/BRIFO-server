package com.brifo.server.agent.entity

import com.brifo.server.user.entity.OAuthProvider
import com.brifo.server.user.entity.User
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AgentExperienceTest {
    @Test
    fun `레벨업 후 초과 경험치를 이월한다`() {
        val agent = createAgent()

        assertTrue(agent.addExperience(130))

        assertEquals(2, agent.level)
        assertEquals(30, agent.exp)
    }

    @Test
    fun `10레벨에 도달하면 경험치를 0으로 만들고 더 이상 누적하지 않는다`() {
        val agent = createAgent()
        repeat(8) { agent.addExperience(100) }
        agent.addExperience(80)

        assertTrue(agent.addExperience(50))
        assertEquals(10, agent.level)
        assertEquals(0, agent.exp)

        assertFalse(agent.addExperience(50))
        assertEquals(0, agent.exp)
    }

    private fun createAgent(): Agent {
        val user = User.create(OAuthProvider.KAKAO, "social-id", "user@example.com")
        return Agent.create(user, AgentType.ROOKIE, "model", "루키", null, 10)
    }
}
