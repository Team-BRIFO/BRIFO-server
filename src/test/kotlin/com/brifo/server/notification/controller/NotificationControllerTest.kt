package com.brifo.server.notification.controller

import com.brifo.server.authenticatedUserId
import com.brifo.server.global.common.CursorPage
import com.brifo.server.notification.dto.request.GetNotificationsRequest
import com.brifo.server.notification.dto.response.GetNotificationsResponse
import com.brifo.server.notification.service.NotificationService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@WebMvcTest(NotificationController::class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var notificationService: NotificationService

    @Test
    fun `목록 조회는 기본 size와 UUID userId를 받는다`() {
        val userId = authenticatedUserId()
        val response = GetNotificationsResponse(CursorPage(emptyList(), null, false))
        `when`(notificationService.getNotifications(userId, GetNotificationsRequest())).thenReturn(response)

        mockMvc
            .perform(get("/api/notifications").param("userId", userId.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("COMMON_200"))
            .andExpect(jsonPath("$.result.page.items").isEmpty)
            .andExpect(jsonPath("$.result.page.hasNext").value(false))
    }

    @Test
    fun `잘못된 size와 cursor는 COMMON_400이고 userId 요청 파라미터는 무시한다`() {
        val userId = authenticatedUserId()
        listOf("0", "51").forEach { size ->
            mockMvc
                .perform(
                    get("/api/notifications")
                        .param("userId", userId.toString())
                        .param("size", size),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("COMMON_400"))
        }

        mockMvc
            .perform(get("/api/notifications").param("userId", "invalid"))
            .andExpect(status().isOk)

        mockMvc
            .perform(
                get("/api/notifications")
                    .param("userId", userId.toString())
                    .param("cursor", "invalid"),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("COMMON_400"))

        mockMvc
            .perform(get("/api/notifications"))
            .andExpect(status().isOk)
    }
}
