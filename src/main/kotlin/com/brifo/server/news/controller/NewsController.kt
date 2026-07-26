package com.brifo.server.news.controller

import com.brifo.server.global.common.ApiResponse
import com.brifo.server.global.code.SuccessCode
import com.brifo.server.news.dto.response.GetNewsCardResponse
import com.brifo.server.news.service.NewsService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

// TODO: Spring Security 적용 후 userId 요청 파라미터를 인증 사용자 정보로 교체
@RestController
@RequestMapping("/api/news")
class NewsController(
    private val newsService: NewsService,
) {
    @GetMapping("/{cardId}")
    fun getNewsCard(
        @PathVariable cardId: UUID,
        @RequestParam userId: UUID,
    ): ApiResponse<GetNewsCardResponse> =
        ApiResponse.success(
            code = SuccessCode.OK,
            result = newsService.getNewsCard(cardId),
        )
}
