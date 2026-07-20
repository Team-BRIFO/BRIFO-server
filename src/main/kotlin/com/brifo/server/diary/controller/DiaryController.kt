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
import jakarta.validation.Valid
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
) {
    @GetMapping
    fun getDiaries(
        @Valid @ModelAttribute request: GetDiariesRequest,
    ): ApiResponse<GetDiariesResponse> = TODO("결정일기 목록 조회 서비스 구현 필요")

    @GetMapping("/{diaryId}")
    fun getDiaryDetail(
        @PathVariable diaryId: UUID,
    ): ApiResponse<GetDiaryDetailResponse> = TODO("결정일기 상세 조회 서비스 구현 필요")

    @GetMapping("/calendar")
    fun getDiaryCalendar(
        @Valid @ModelAttribute request: GetDiaryCalendarRequest,
    ): ApiResponse<GetDiaryCalendarResponse> = TODO("결정일기 캘린더 조회 서비스 구현 필요")

    @GetMapping("/stats")
    fun getDiaryStats(): ApiResponse<GetDiaryStatsResponse> = TODO("결정일기 통계 조회 서비스 구현 필요")

    @PostMapping("/{diaryId}/share-images")
    fun createDiaryShareImage(
        @PathVariable diaryId: UUID,
    ): ApiResponse<CreateDiaryShareImageResponse> = TODO("결정일기 공유 이미지 생성 서비스 구현 필요")
}
