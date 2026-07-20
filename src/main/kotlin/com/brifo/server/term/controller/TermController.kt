package com.brifo.server.term.controller

import com.brifo.server.global.common.ApiResponse
import com.brifo.server.global.code.SuccessCode
import com.brifo.server.term.dto.request.GetMyTermsRequest
import com.brifo.server.term.dto.response.GetMyTermsResponse
import com.brifo.server.term.dto.response.GetTermResponse
import com.brifo.server.term.service.TermService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api")
class TermController(
    private val termService: TermService,
) {
    @GetMapping("/users/me/terms")
    fun getMyTerms(
        @RequestParam userId: Long,
        @Valid @ModelAttribute request: GetMyTermsRequest,
    ): ApiResponse<GetMyTermsResponse> =
        ApiResponse.success(
            code = SuccessCode.OK,
            result = termService.getMyTerms(userId, request),
        )

    @GetMapping("/terms/{termId}")
    fun getTerm(
        @RequestParam userId: Long,
        @PathVariable termId: UUID,
    ): ApiResponse<GetTermResponse> =
        ApiResponse.success(
            code = SuccessCode.OK,
            result = termService.getTerm(userId, termId),
        )

    @PutMapping("/users/me/terms/{termId}")
    fun saveTerm(
        @RequestParam userId: Long,
        @PathVariable termId: UUID,
    ): ApiResponse<Nothing> {
        termService.saveTerm(userId, termId)
        return ApiResponse.success(SuccessCode.OK)
    }
}
