package com.brifo.server.agent.controller

import com.brifo.server.agent.dto.response.GetAgentDetailResponse
import com.brifo.server.agent.dto.response.GetAgentsResponse
import com.brifo.server.agent.service.AgentService
import com.brifo.server.global.common.ApiResponse
import com.brifo.server.global.code.SuccessCode
import org.springframework.security.core.annotation.AuthenticationPrincipal
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
    fun getAgents(
        @AuthenticationPrincipal userPublicId: UUID,
    ): ApiResponse<GetAgentsResponse> =
        ApiResponse.success(SuccessCode.OK, agentService.getAgents(userPublicId))

    @GetMapping("/{agentId}")
    fun getAgentDetail(
        @PathVariable agentId: UUID,
        @AuthenticationPrincipal userPublicId: UUID,
    ): ApiResponse<GetAgentDetailResponse> =
        ApiResponse.success(SuccessCode.OK, agentService.getAgentDetail(userPublicId, agentId))
}
