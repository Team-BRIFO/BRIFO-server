package com.brifo.server.news.controller

import com.brifo.server.global.common.ApiResponse
import com.brifo.server.news.dto.response.GetNewsCardResponse
import com.brifo.server.news.service.NewsService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/news")
class NewsController(
    private val newsService: NewsService,
) {
    @GetMapping("/{cardId}")
    fun getNewsCard(
        @PathVariable cardId: UUID,
    ): ApiResponse<GetNewsCardResponse> = TODO("News 카드 상세 조회 서비스 구현 필요")
}
