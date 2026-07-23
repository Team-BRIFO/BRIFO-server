package com.brifo.server.diary.controller

import com.brifo.server.diary.dto.request.GetDiariesRequest
import com.brifo.server.diary.dto.request.GetDiaryCalendarRequest
import com.brifo.server.diary.dto.response.CreateDiaryShareImageResponse
import com.brifo.server.diary.dto.response.GetDiariesResponse
import com.brifo.server.diary.dto.response.GetDiaryCalendarResponse
import com.brifo.server.diary.dto.response.GetDiaryDetailResponse
import com.brifo.server.diary.dto.response.GetDiaryStatsResponse
import com.brifo.server.diary.service.DiaryService
import com.brifo.server.global.common.ApiResponse
import com.brifo.server.global.code.SuccessCode
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/diaries")
class DiaryController(
    private val diaryService: DiaryService,
) {
    @GetMapping
    fun getDiaries(
        @Valid @ModelAttribute request: GetDiariesRequest,
        @RequestParam userId: UUID,
    ): ApiResponse<GetDiariesResponse> =
        ApiResponse.success(
            SuccessCode.OK,
            diaryService.getDiaries(userId, request),
        )

    @GetMapping("/{diaryId}")
    fun getDiaryDetail(
        @PathVariable diaryId: UUID,
        @RequestParam userId: UUID,
    ): ApiResponse<GetDiaryDetailResponse> =
        ApiResponse.success(
            SuccessCode.OK,
            diaryService.getDiaryDetail(userId, diaryId),
        )

    @GetMapping("/calendar")
    fun getDiaryCalendar(
        @Valid @ModelAttribute request: GetDiaryCalendarRequest,
        @RequestParam userId: UUID,
    ): ApiResponse<GetDiaryCalendarResponse> =
        ApiResponse.success(
            SuccessCode.OK,
            diaryService.getDiaryCalendar(userId, request),
        )

    @GetMapping("/stats")
    fun getDiaryStats(
        @RequestParam userId: UUID,
    ): ApiResponse<GetDiaryStatsResponse> =
        ApiResponse.success(
            SuccessCode.OK,
            diaryService.getDiaryStats(userId),
        )

    @PostMapping("/{diaryId}/share-images")
    fun createDiaryShareImage(
        @PathVariable diaryId: UUID,
    ): ApiResponse<CreateDiaryShareImageResponse> = TODO("결정일기 공유 이미지 생성 서비스 구현 필요")
}
