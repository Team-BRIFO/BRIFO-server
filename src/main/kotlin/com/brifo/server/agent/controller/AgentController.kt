package com.brifo.server.agent.controller

import com.brifo.server.agent.dto.response.GetAgentDetailResponse
import com.brifo.server.agent.dto.response.GetAgentsResponse
import com.brifo.server.agent.service.AgentService
import com.brifo.server.global.common.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/agents")
class AgentController(
    private val agentService: AgentService,
) {
    @GetMapping
    fun getAgents(): ApiResponse<GetAgentsResponse> = TODO("Agent 목록 조회 서비스 구현 필요")

    @GetMapping("/{agentId}")
    fun getAgentDetail(
        @PathVariable agentId: UUID,
    ): ApiResponse<GetAgentDetailResponse> = TODO("Agent 상세 조회 서비스 구현 필요")
}
