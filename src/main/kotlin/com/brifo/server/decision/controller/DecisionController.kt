package com.brifo.server.decision.controller

import com.brifo.server.decision.dto.request.CreateDecisionRequest
import com.brifo.server.decision.dto.response.CreateDecisionResponse
import com.brifo.server.decision.dto.response.GetDecisionResultResponse
import com.brifo.server.decision.dto.response.GetDecisionsResponse
import com.brifo.server.decision.service.DecisionQueryService
import com.brifo.server.decision.service.DecisionRequestService
import com.brifo.server.global.common.ApiResponse
import com.brifo.server.global.code.SuccessCode
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
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
    private val decisionRequestService: DecisionRequestService,
    private val decisionQueryService: DecisionQueryService,
) {
    @PostMapping("/briefings/{briefingId}/decisions")
    @ResponseStatus(HttpStatus.CREATED)
    fun createDecision(
        @PathVariable briefingId: UUID,
        @Valid @RequestBody request: CreateDecisionRequest,
        @AuthenticationPrincipal userPublicId: UUID,
    ): ApiResponse<CreateDecisionResponse> =
        ApiResponse.success(
            SuccessCode.CREATED,
            decisionRequestService.request(
                userPublicId = userPublicId,
                briefingPublicId = briefingId,
                direction = request.direction,
                confidenceLevel = request.confidenceLevel,
            ),
        )

    @GetMapping("/decisions")
    fun getDecisions(
        @AuthenticationPrincipal userPublicId: UUID,
    ): ApiResponse<GetDecisionsResponse> =
        ApiResponse.success(
            SuccessCode.OK,
            decisionQueryService.getDecisions(userPublicId),
        )

    @GetMapping("/decisions/{decisionId}")
    fun getDecisionResult(
        @PathVariable decisionId: UUID,
        @AuthenticationPrincipal userPublicId: UUID,
    ): ApiResponse<GetDecisionResultResponse> =
        ApiResponse.success(
            SuccessCode.OK,
            decisionQueryService.getDecisionResult(userPublicId, decisionId),
        )
}
