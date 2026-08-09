package com.brifo.server.externalapi

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(DataProviderProperties::class)
class DataProviderConfiguration
