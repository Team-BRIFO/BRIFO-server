package com.brifo.server

import com.brifo.server.briefing.support.BriefingTestConfiguration
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import

@TestConfiguration(proxyBeanMethods = false)
@Import(
    TestcontainersConfiguration::class,
    BriefingTestConfiguration::class,
)
class ServerTestConfiguration
