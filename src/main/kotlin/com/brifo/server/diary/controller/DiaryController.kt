package com.brifo.server.diary.controller

import com.brifo.server.diary.code.DiarySuccessCode
import com.brifo.server.diary.dto.request.GetDiariesRequest
import com.brifo.server.diary.dto.request.GetDiaryCalendarRequest
import com.brifo.server.diary.dto.response.CreateDiaryShareImageResponse
import com.brifo.server.diary.dto.response.GetDiariesResponse
import com.brifo.server.diary.dto.response.GetDiaryCalendarResponse
import com.brifo.server.diary.dto.response.GetDiaryDetailResponse
import com.brifo.server.diary.dto.response.GetDiaryStatsResponse
import com.brifo.server.diary.service.DiaryService
import com.brifo.server.diary.service.DiaryShareImageService
import com.brifo.server.global.code.SuccessCode
import com.brifo.server.global.common.ApiResponse
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/diaries")
class DiaryController(
    private val diaryService: DiaryService,
    private val diaryShareImageService: DiaryShareImageService,
) {
    @GetMapping
    fun getDiaries(
        @Valid @ModelAttribute request: GetDiariesRequest,
        @AuthenticationPrincipal userPublicId: UUID,
    ): ApiResponse<GetDiariesResponse> =
        ApiResponse.success(
            SuccessCode.OK,
            diaryService.getDiaries(userPublicId, request),
        )

    @GetMapping("/{diaryId}")
    fun getDiaryDetail(
        @PathVariable diaryId: UUID,
        @AuthenticationPrincipal userPublicId: UUID,
    ): ApiResponse<GetDiaryDetailResponse> =
        ApiResponse.success(
            SuccessCode.OK,
            diaryService.getDiaryDetail(userPublicId, diaryId),
        )

    @GetMapping("/calendar")
    fun getDiaryCalendar(
        @Valid @ModelAttribute request: GetDiaryCalendarRequest,
        @AuthenticationPrincipal userPublicId: UUID,
    ): ApiResponse<GetDiaryCalendarResponse> =
        ApiResponse.success(
            SuccessCode.OK,
            diaryService.getDiaryCalendar(userPublicId, request),
        )

    @GetMapping("/stats")
    fun getDiaryStats(
        @AuthenticationPrincipal userPublicId: UUID,
    ): ApiResponse<GetDiaryStatsResponse> =
        ApiResponse.success(
            SuccessCode.OK,
            diaryService.getDiaryStats(userPublicId),
        )

    @PostMapping("/{diaryId}/share-images")
    fun createDiaryShareImage(
        @PathVariable diaryId: UUID,
        @AuthenticationPrincipal userPublicId: UUID,
    ): ApiResponse<CreateDiaryShareImageResponse> =
        ApiResponse.success(
            DiarySuccessCode.SHARE_IMAGE_CREATED,
            diaryShareImageService.create(userPublicId, diaryId),
        )
}
