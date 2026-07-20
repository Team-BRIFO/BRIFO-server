package com.brifo.server

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@Import(ServerTestConfiguration::class)
@ActiveProfiles("test")
@SpringBootTest
class ServerApplicationTests {
    @Test
    fun contextLoads() {
    }
}
