package com.brifo.server.diary.share

import org.springframework.stereotype.Component
import java.awt.Color
import java.awt.Font
import java.awt.GradientPaint
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

@Component
class Java2dDiaryShareImageRenderer(
    private val properties: DiaryShareImageProperties,
) : DiaryShareImageRenderer {
    override fun render(model: DiaryShareImageModel): ShareImageFile {
        require(properties.width >= 600 && properties.height >= 315) {
            "share image dimensions are too small"
        }

        val image = BufferedImage(properties.width, properties.height, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            graphics.paint =
                GradientPaint(
                    0f,
                    0f,
                    Color(23, 28, 48),
                    properties.width.toFloat(),
                    properties.height.toFloat(),
                    Color(55, 39, 92),
                )
            graphics.fillRect(0, 0, properties.width, properties.height)

            val padding = properties.width / 12
            graphics.color = Color(255, 255, 255, 220)
            graphics.font = Font(properties.fontFamily, Font.BOLD, properties.height / 18)
            graphics.drawString("BRIFO 결정일기", padding, properties.height / 7)

            graphics.color = Color.WHITE
            graphics.font = Font(properties.fontFamily, Font.BOLD, properties.height / 10)
            graphics.drawString(model.stockName, padding, properties.height * 2 / 5)

            val resultText = if (model.isCorrect) "예측 성공" else "예측 아쉬움"
            graphics.color = if (model.isCorrect) Color(83, 232, 181) else Color(255, 155, 155)
            graphics.font = Font(properties.fontFamily, Font.BOLD, properties.height / 13)
            graphics.drawString(resultText, padding, properties.height * 11 / 20)

            graphics.color = Color(255, 255, 255, 210)
            graphics.font = Font(properties.fontFamily, Font.PLAIN, properties.height / 24)
            graphics.drawString(
                "${model.agentNickname} · ${model.briefingDirection} ${model.briefingConfidenceRate}%",
                padding,
                properties.height * 7 / 10,
            )
            graphics.drawString(
                "등락률 ${model.changeRate}% · 나의 확신도 ${model.decisionConfidenceLevel}/5",
                padding,
                properties.height * 4 / 5,
            )
        } finally {
            graphics.dispose()
        }

        val output = ByteArrayOutputStream()
        check(ImageIO.write(image, "png", output)) { "PNG writer is unavailable" }
        return ShareImageFile(output.toByteArray(), "image/png", "png")
    }
}
