package com.brifo.server.external.log.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import com.fasterxml.jackson.databind.ObjectMapper

// 생성자 주입을 하기 위해 ObjectMapper 객체를 Spring에 등록하는 코드
@Configuration
class ExternalApiCallLogConfig {
    @Bean
    fun externalApiCallLogObjectMapper(): ObjectMapper {
        return ObjectMapper()
    }
}
