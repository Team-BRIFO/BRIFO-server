package com.brifo.server.briefing.controller

import com.brifo.server.briefing.dto.request.CreateBriefingRequest
import com.brifo.server.briefing.dto.request.GetBriefingsRequest
import com.brifo.server.briefing.dto.response.CreateBriefingResponse
import com.brifo.server.briefing.dto.response.GetBriefingDetailResponse
import com.brifo.server.briefing.dto.response.GetBriefingsResponse
import com.brifo.server.briefing.dto.response.GetCardBriefingsResponse
import com.brifo.server.briefing.dto.response.GetOfficeBriefingsResponse
import com.brifo.server.briefing.service.BriefingService
import com.brifo.server.global.common.ApiResponse
import com.brifo.server.global.code.SuccessCode
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api")
class BriefingController(
    private val briefingService: BriefingService,
) {
    @PostMapping("/news/{cardId}/briefings")
    @ResponseStatus(HttpStatus.CREATED)
    fun createBriefing(
        @PathVariable cardId: UUID,
        @Valid @RequestBody request: CreateBriefingRequest,
    ): ApiResponse<CreateBriefingResponse> = TODO("Briefing 생성 서비스 구현 필요")

    @GetMapping("/news/{cardId}/briefing")
    fun getCardBriefings(
        @PathVariable cardId: UUID,
    ): ApiResponse<GetCardBriefingsResponse> = TODO("카드뉴스별 Briefing 조회 서비스 구현 필요")

    @GetMapping("/briefings/office")
    fun getOfficeBriefings(): ApiResponse<GetOfficeBriefingsResponse> =
        ApiResponse.success(SuccessCode.OK, briefingService.getOfficeBriefings())

    @GetMapping("/briefings/{briefingId}")
    fun getBriefingDetail(
        @PathVariable briefingId: UUID,
    ): ApiResponse<GetBriefingDetailResponse> = TODO("Briefing 상세 조회 서비스 구현 필요")

    @GetMapping("/briefings")
    fun getBriefings(
        @Valid @ModelAttribute request: GetBriefingsRequest,
    ): ApiResponse<GetBriefingsResponse> = TODO("Briefing 목록 조회 서비스 구현 필요")
}
