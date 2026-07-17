package com.brifo.server.term.controller

import com.brifo.server.global.common.CursorPage
import com.brifo.server.term.dto.request.GetMyTermsRequest
import com.brifo.server.term.dto.response.GetMyTermsResponse
import com.brifo.server.term.dto.response.GetTermResponse
import com.brifo.server.term.exception.TermNotFoundException
import com.brifo.server.term.service.TermService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import java.util.UUID

@WebMvcTest(TermController::class)
@AutoConfigureMockMvc(addFilters = false)
class TermControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var termService: TermService

    @Test
    fun `내 용어장 조회는 size 기본값과 경계값을 허용한다`() {
        val emptyResponse =
            GetMyTermsResponse(
                learnedTermCount = 0,
                page =
                    CursorPage(
                        items = emptyList(),
                        nextCursor = null,
                        hasNext = false,
                    ),
            )
        listOf(20, 1, 50).forEach { size ->
            `when`(termService.getMyTerms(userId = 7L, request = GetMyTermsRequest(size = size)))
                .thenReturn(emptyResponse)
        }

        mockMvc
            .perform(
                get("/api/users/me/terms")
                    .param("userId", "7"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("COMMON_200"))
            .andExpect(jsonPath("$.result.learnedTermCount").value(0))
            .andExpect(jsonPath("$.result.page.items").isEmpty)
            .andExpect(jsonPath("$.result.page.hasNext").value(false))
        listOf("1", "50").forEach { size ->
            mockMvc
                .perform(
                    get("/api/users/me/terms")
                        .param("userId", "7")
                        .param("size", size),
                ).andExpect(status().isOk)
        }
    }

    @Test
    fun `학습 기록 ID는 다음 커서에 사용되지만 항목에는 노출하지 않는다`() {
        val learnedTermId = UUID.randomUUID()
        val termId = UUID.randomUUID()
        val response =
            GetMyTermsResponse(
                learnedTermCount = 2,
                page =
                    CursorPage(
                        items =
                            listOf(
                                GetMyTermsResponse.Item(
                                    learnedTermId = learnedTermId,
                                    termId = termId,
                                    term = "PER",
                                    definition = "주가수익비율",
                                    category = "지표",
                                    learnedAt = LocalDateTime.of(2026, 7, 16, 10, 0),
                                ),
                            ),
                        nextCursor = learnedTermId,
                        hasNext = true,
                    ),
            )
        `when`(termService.getMyTerms(7L, GetMyTermsRequest(size = 1))).thenReturn(response)

        mockMvc
            .perform(
                get("/api/users/me/terms")
                    .param("userId", "7")
                    .param("size", "1"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.result.page.items[0].learnedTermId").doesNotExist())
            .andExpect(jsonPath("$.result.page.items[0].termId").value(termId.toString()))
            .andExpect(jsonPath("$.result.page.nextCursor").value(learnedTermId.toString()))
    }

    @Test
    fun `내 용어장 size 경계 밖의 0과 51은 COMMON_400이다`() {
        listOf("0", "51").forEach { size ->
            mockMvc
                .perform(
                    get("/api/users/me/terms")
                        .param("userId", "7")
                        .param("size", size),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
        }
    }

    @Test
    fun `잘못된 cursor UUID는 COMMON_400이다`() {
        mockMvc
            .perform(
                get("/api/users/me/terms")
                    .param("userId", "7")
                    .param("cursor", "not-a-uuid"),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("COMMON_400"))
    }

    @Test
    fun `userId가 없거나 숫자가 아니면 COMMON_400이다`() {
        mockMvc
            .perform(get("/api/users/me/terms"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("COMMON_400"))

        mockMvc
            .perform(
                get("/api/users/me/terms")
                    .param("userId", "invalid"),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("COMMON_400"))
    }

    @Test
    fun `용어 상세 조회 성공 응답은 학습 여부를 포함한다`() {
        val termId = UUID.randomUUID()
        `when`(termService.getTerm(7L, termId)).thenReturn(
            GetTermResponse(
                termId = termId,
                term = "PER",
                definition = "주가수익비율",
                category = "지표",
                isLearned = true,
            ),
        )

        mockMvc
            .perform(
                get("/api/terms/{termId}", termId)
                    .param("userId", "7"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.result.termId").value(termId.toString()))
            .andExpect(jsonPath("$.result.term").value("PER"))
            .andExpect(jsonPath("$.result.isLearned").value(true))
    }

    @Test
    fun `잘못된 path UUID는 COMMON_400이다`() {
        mockMvc
            .perform(
                get("/api/terms/{termId}", "not-a-uuid")
                    .param("userId", "7"),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("COMMON_400"))
    }

    @Test
    fun `존재하지 않는 용어 상세 조회는 TERM_404이다`() {
        val termId = UUID.randomUUID()
        `when`(termService.getTerm(7L, termId)).thenThrow(TermNotFoundException())

        mockMvc
            .perform(
                get("/api/terms/{termId}", termId)
                    .param("userId", "7"),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("TERM_404"))
    }

    @Test
    fun `용어 저장 성공 응답에는 result가 없다`() {
        val termId = UUID.randomUUID()

        mockMvc
            .perform(
                put("/api/users/me/terms/{termId}", termId)
                    .param("userId", "7"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("COMMON_200"))
            .andExpect(jsonPath("$.result").doesNotExist())

        verify(termService).saveTerm(7L, termId)
    }
}
