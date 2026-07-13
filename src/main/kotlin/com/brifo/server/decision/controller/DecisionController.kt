package com.brifo.server.decision.controller

import com.brifo.server.decision.dto.request.CreateDecisionRequest
import com.brifo.server.decision.dto.response.CreateDecisionResponse
import com.brifo.server.decision.dto.response.GetDecisionResultResponse
import com.brifo.server.decision.dto.response.GetDecisionsResponse
import com.brifo.server.decision.service.DecisionService
import com.brifo.server.global.common.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api")
class DecisionController(
    private val decisionService: DecisionService,
) {
    @PostMapping("/briefings/{briefingId}/decisions")
    @ResponseStatus(HttpStatus.CREATED)
    fun createDecision(
        @PathVariable briefingId: UUID,
        @Valid @RequestBody request: CreateDecisionRequest,
    ): ApiResponse<CreateDecisionResponse> = TODO("Decision 생성 서비스 구현 필요")

    @GetMapping("/decisions")
    fun getDecisions(): ApiResponse<GetDecisionsResponse> = TODO("Decision 목록 조회 서비스 구현 필요")

    @GetMapping("/decisions/{decisionId}")
    fun getDecisionResult(
        @PathVariable decisionId: UUID,
    ): ApiResponse<GetDecisionResultResponse> = TODO("Decision 결과 조회 서비스 구현 필요")
}
