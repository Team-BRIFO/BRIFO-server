package com.brifo.server.batch.dev

import com.brifo.server.global.code.SuccessCode
import com.brifo.server.global.common.ApiResponse
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@Profile("dev")
@ConditionalOnProperty(prefix = "app.dev-auth", name = ["enabled"], havingValue = "true")
@RequestMapping("/api/dev/batches")
class DevBatchController(
    private val service: DevBatchService,
) {
    @PostMapping("/news-collection/rerun")
    fun rerunNewsCollection(
        @AuthenticationPrincipal userPublicId: UUID,
        @RequestBody request: DevNewsCollectionBatchRequest,
    ): ApiResponse<DevBatchRunResponse> =
        ApiResponse.success(SuccessCode.OK, service.rerunNewsCollection(userPublicId, request))

    @PostMapping("/news-card-generation/rerun")
    fun rerunNewsCardGeneration(
        @AuthenticationPrincipal userPublicId: UUID,
        @RequestBody request: DevDateBatchRequest,
    ): ApiResponse<DevBatchRunResponse> =
        ApiResponse.success(SuccessCode.OK, service.rerunNewsCardGeneration(userPublicId, request.targetDate))

    @PostMapping("/decision-settlement/rerun")
    fun rerunDecisionSettlement(
        @AuthenticationPrincipal userPublicId: UUID,
        @RequestBody request: DevDateBatchRequest,
    ): ApiResponse<DevBatchRunResponse> =
        ApiResponse.success(SuccessCode.OK, service.rerunDecisionSettlement(userPublicId, request.targetDate))
}
