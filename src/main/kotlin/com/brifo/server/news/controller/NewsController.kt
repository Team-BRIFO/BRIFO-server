package com.brifo.server.news.controller

import com.brifo.server.global.common.ApiResponse
import com.brifo.server.global.code.SuccessCode
import com.brifo.server.news.dto.response.GetNewsCardsResponse
import com.brifo.server.news.service.NewsService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/stocks")
class NewsController(
    private val newsService: NewsService,
) {
    @GetMapping("/{stockId}/news-cards")
    fun getNewsCards(
        @PathVariable stockId: UUID,
    ): ApiResponse<GetNewsCardsResponse> =
        ApiResponse.success(
            code = SuccessCode.OK,
            result = newsService.getNewsCards(stockId),
        )
}
