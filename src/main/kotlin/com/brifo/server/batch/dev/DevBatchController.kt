package com.brifo.server.batch.dev

import com.brifo.server.global.code.SuccessCode
import com.brifo.server.global.common.ApiResponse
import jakarta.validation.Valid
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Profile("dev")
@ConditionalOnProperty(prefix = "app.dev-auth", name = ["enabled"], havingValue = "true")
@RequestMapping("/api/dev/batches")
class DevBatchController(
    private val service: DevBatchService,
) {
    @PostMapping("/news-collection/rerun")
    fun rerunNewsCollection(
        @Valid @RequestBody request: DevDateBatchRequest,
    ): ApiResponse<DevBatchRunResponse> =
        ApiResponse.success(SuccessCode.OK, service.rerunNewsCollection(request))

    @PostMapping("/news-card-generation/rerun")
    fun rerunNewsCardGeneration(
        @Valid @RequestBody request: DevDateBatchRequest,
    ): ApiResponse<DevBatchRunResponse> =
        ApiResponse.success(SuccessCode.OK, service.rerunNewsCardGeneration(request))

    @PostMapping("/news-card-generation/run-one")
    fun runSingleNewsCardGeneration(
        @Valid @RequestBody request: DevSingleNewsCardGenerationRequest,
    ): ApiResponse<DevSingleNewsCardGenerationResponse> =
        ApiResponse.success(SuccessCode.OK, service.runSingleNewsCardGeneration(request))

    @PostMapping("/decision-settlement/rerun")
    fun rerunDecisionSettlement(
        @Valid @RequestBody request: DevDateBatchRequest,
    ): ApiResponse<DevBatchRunResponse> =
        ApiResponse.success(SuccessCode.OK, service.rerunDecisionSettlement(request))
}
