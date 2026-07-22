package com.brifo.server.agent.controller

import com.brifo.server.agent.dto.response.GetAgentsResponse
import com.brifo.server.agent.entity.AgentType
import com.brifo.server.agent.exception.AgentNotFoundException
import com.brifo.server.agent.service.AgentService
import com.brifo.server.global.error.GlobalExceptionHandler
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.mock.env.MockEnvironment
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.util.UUID

class AgentControllerTest {
    private val service = mock(AgentService::class.java)
    private val mockMvc: MockMvc = MockMvcBuilders
        .standaloneSetup(AgentController(service))
        .setControllerAdvice(GlobalExceptionHandler(MockEnvironment()))
        .build()

    @Test
    fun `사원 목록은 임시 userId로 소유 사원을 조회한다`() {
        val userId = UUID.randomUUID()
        val agentId = UUID.randomUUID()
        `when`(service.getAgents(userId)).thenReturn(
            GetAgentsResponse(
                listOf(
                    GetAgentsResponse.Item(
                        agentId = agentId,
                        nickname = "루키",
                        agentType = AgentType.ROOKIE,
                        modelName = "gpt-4.1-mini",
                        level = 1,
                        exp = 120,
                        dailySalary = 10,
                        accuracyRate = 67,
                    ),
                ),
            ),
        )

        mockMvc.perform(get("/api/agents").param("userId", userId.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("COMMON_200"))
            .andExpect(jsonPath("$.result.items[0].agentId").value(agentId.toString()))
            .andExpect(jsonPath("$.result.items[0].accuracyRate").value(67))
    }

    @Test
    fun `소유하지 않은 사원 상세는 AGENT_404를 반환한다`() {
        val userId = UUID.randomUUID()
        val agentId = UUID.randomUUID()
        `when`(service.getAgentDetail(userId, agentId)).thenThrow(AgentNotFoundException())

        mockMvc.perform(
            get("/api/agents/{agentId}", agentId)
                .param("userId", userId.toString()),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("AGENT_404"))
    }
}
