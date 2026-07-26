package com.brifo.server.agent.controller

import com.brifo.server.agent.dto.response.GetAgentDetailResponse
import com.brifo.server.agent.dto.response.GetAgentsResponse
import com.brifo.server.agent.service.AgentService
import com.brifo.server.global.common.ApiResponse
import com.brifo.server.global.code.SuccessCode
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/agents")
class AgentController(
    private val agentService: AgentService,
) {
    @GetMapping
    fun getAgents(
        @RequestParam userId: UUID,
    ): ApiResponse<GetAgentsResponse> =
        ApiResponse.success(SuccessCode.OK, agentService.getAgents(userId))

    @GetMapping("/{agentId}")
    fun getAgentDetail(
        @PathVariable agentId: UUID,
        @RequestParam userId: UUID,
    ): ApiResponse<GetAgentDetailResponse> =
        ApiResponse.success(SuccessCode.OK, agentService.getAgentDetail(userId, agentId))
}
