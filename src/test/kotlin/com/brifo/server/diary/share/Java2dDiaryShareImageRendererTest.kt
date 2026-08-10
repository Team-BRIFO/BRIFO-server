package com.brifo.server.diary.share

import com.brifo.server.agent.entity.AgentType
import com.brifo.server.briefing.entity.BriefingDirection
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import java.util.UUID
import javax.imageio.ImageIO
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Java2dDiaryShareImageRendererTest {
    @Test
    fun `설정된 크기의 PNG 공유 이미지를 생성한다`() {
        val renderer =
            Java2dDiaryShareImageRenderer(
                DiaryShareImageProperties(width = 1200, height = 630),
            )

        val result =
            renderer.render(
                DiaryShareImageModel(
                    diaryId = UUID.randomUUID(),
                    stockName = "삼성전자",
                    changeRate = BigDecimal("2.5"),
                    agentType = AgentType.ROOKIE,
                    agentNickname = "루키",
                    briefingDirection = BriefingDirection.UP,
                    briefingConfidenceRate = 72,
                    isCorrect = true,
                    decisionConfidenceLevel = 4,
                ),
            )

        val image = ImageIO.read(ByteArrayInputStream(result.bytes))
        assertEquals("image/png", result.contentType)
        assertEquals("png", result.extension)
        assertContentEquals(byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10), result.bytes.take(8).toByteArray())
        assertEquals(1200, image.width)
        assertEquals(630, image.height)
        assertTrue(result.bytes.isNotEmpty())
    }
}
