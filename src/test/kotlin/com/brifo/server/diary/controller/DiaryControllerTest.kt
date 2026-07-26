package com.brifo.server.diary.controller

import com.brifo.server.diary.service.DiaryService
import com.brifo.server.global.error.GlobalExceptionHandler
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.mock.env.MockEnvironment
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import java.util.UUID

class DiaryControllerTest {
    private val service = mock(DiaryService::class.java)
    private val userId = UUID.randomUUID()
    private val mockMvc = run {
        val validator = LocalValidatorFactoryBean().also { it.afterPropertiesSet() }
        MockMvcBuilders
            .standaloneSetup(DiaryController(service))
            .setControllerAdvice(GlobalExceptionHandler(MockEnvironment()))
            .setValidator(validator)
            .build()
    }

    @Test
    fun `목록 크기가 범위를 벗어나면 400을 반환한다`() {
        mockMvc.perform(
            get("/api/diaries")
                .param("userId", userId.toString())
                .param("size", "51"),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("COMMON_400"))
    }

    @Test
    fun `캘린더 월이 범위를 벗어나면 400을 반환한다`() {
        mockMvc.perform(
            get("/api/diaries/calendar")
                .param("userId", userId.toString())
                .param("year", "2026")
                .param("month", "13"),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("COMMON_400"))
    }
}
