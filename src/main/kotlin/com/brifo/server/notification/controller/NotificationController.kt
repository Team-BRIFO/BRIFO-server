package com.brifo.server.notification.controller

import com.brifo.server.global.common.ApiResponse
import com.brifo.server.global.code.SuccessCode
import com.brifo.server.notification.dto.request.GetNotificationsRequest
import com.brifo.server.notification.dto.response.GetNotificationsResponse
import com.brifo.server.notification.service.NotificationService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/notifications")
class NotificationController(
    private val notificationService: NotificationService,
) {
    @GetMapping
    fun getNotifications(
        @RequestParam userId: UUID,
        @Valid @ModelAttribute request: GetNotificationsRequest,
    ): ApiResponse<GetNotificationsResponse> =
        ApiResponse.success(
            code = SuccessCode.OK,
            result = notificationService.getNotifications(userId, request),
        )
}
