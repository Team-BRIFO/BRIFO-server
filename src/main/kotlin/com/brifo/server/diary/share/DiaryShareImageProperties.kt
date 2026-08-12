package com.brifo.server.diary.share

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.share-image")
data class DiaryShareImageProperties(
    val bucket: String = "disabled",
    val publicBaseUrl: String = "",
    val keyPrefix: String = "diary-share-images",
    val region: String = "ap-northeast-2",
    val endpoint: String = "",
    val pathStyleAccess: Boolean = false,
    val fontFamily: String = "Noto Sans CJK KR",
    val width: Int = 1200,
    val height: Int = 630,
)
