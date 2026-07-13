package com.brifo.server.term.controller

import com.brifo.server.global.common.ApiResponse
import com.brifo.server.term.dto.request.GetMyTermsRequest
import com.brifo.server.term.dto.response.GetMyTermsResponse
import com.brifo.server.term.dto.response.GetTermResponse
import com.brifo.server.term.service.TermService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api")
class TermController(
    private val termService: TermService,
) {
    @GetMapping("/users/me/terms")
    fun getMyTerms(
        @Valid @ModelAttribute request: GetMyTermsRequest,
    ): ApiResponse<GetMyTermsResponse> = TODO("내 용어장 조회 서비스 구현 필요")

    @GetMapping("/terms/{termId}")
    fun getTerm(
        @PathVariable termId: UUID,
    ): ApiResponse<GetTermResponse> = TODO("용어 상세 조회 서비스 구현 필요")

    @PostMapping("/users/me/terms/{termId}")
    fun saveTerm(
        @PathVariable termId: UUID,
    ): ApiResponse<Nothing> = TODO("용어 저장 서비스 구현 필요")
}
