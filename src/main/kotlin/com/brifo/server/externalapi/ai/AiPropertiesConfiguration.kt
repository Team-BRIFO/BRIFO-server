package com.brifo.server.externalapi.ai

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(AiProperties::class)
class AiPropertiesConfiguration
