package com.brifo.server.briefing.controller

import com.brifo.server.briefing.dto.request.CreateBriefingRequest
import com.brifo.server.briefing.dto.response.CreateBriefingResponse
import com.brifo.server.briefing.dto.response.GetBriefingDetailResponse
import com.brifo.server.briefing.dto.response.GetOfficeBriefingsResponse
import com.brifo.server.briefing.dto.response.GetStockBriefingsResponse
import com.brifo.server.briefing.service.BriefingQueryService
import com.brifo.server.briefing.service.sync.BriefingRequestOrchestrator
import com.brifo.server.global.common.ApiResponse
import com.brifo.server.global.code.SuccessCode
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

// TODO: Spring Security 적용 후 개발용 userId 요청 파라미터를 인증 사용자 정보로 대체
@RestController
@RequestMapping("/api")
class BriefingController(
    private val briefingQueryService: BriefingQueryService,
    private val briefingRequestOrchestrator: BriefingRequestOrchestrator,
) {
    @PostMapping("/stocks/{stockId}/briefings")
    @ResponseStatus(HttpStatus.CREATED)
    fun createBriefing(
        @PathVariable stockId: UUID,
        @Valid @RequestBody request: CreateBriefingRequest,
        @RequestParam userId: UUID,
    ): ApiResponse<CreateBriefingResponse> =
        ApiResponse.success(
            SuccessCode.CREATED,
            briefingRequestOrchestrator.request(
                userPublicId = userId,
                stockPublicId = stockId,
                agentPublicIds = request.agentIds,
            ),
        )

    @GetMapping("/stocks/{stockId}/briefing")
    fun getStockBriefings(
        @PathVariable stockId: UUID,
        @RequestParam userId: UUID,
    ): ApiResponse<GetStockBriefingsResponse> =
        ApiResponse.success(
            SuccessCode.OK,
            briefingQueryService.getStockBriefings(
                userPublicId = userId,
                stockPublicId = stockId,
            ),
        )

    @GetMapping("/briefings/office")
    fun getOfficeBriefings(@RequestParam userId: UUID): ApiResponse<GetOfficeBriefingsResponse> =
        ApiResponse.success(
            SuccessCode.OK,
            briefingQueryService.getOfficeBriefings(userId),
        )

    @GetMapping("/briefings/{briefingId}")
    fun getBriefingDetail(
        @PathVariable briefingId: UUID,
        @RequestParam userId: UUID,
    ): ApiResponse<GetBriefingDetailResponse> =
        ApiResponse.success(
            SuccessCode.OK,
            briefingQueryService.getBriefingDetail(
                userPublicId = userId,
                briefingPublicId = briefingId,
            ),
        )
}
