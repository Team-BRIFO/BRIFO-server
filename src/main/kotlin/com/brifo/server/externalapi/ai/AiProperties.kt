package com.brifo.server.externalapi.ai

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("external.ai")
data class AiProperties(
    val baseUrl: String = "disabled",
    val apiKey: String = "disabled",
)
