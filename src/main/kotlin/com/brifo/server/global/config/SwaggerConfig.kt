package com.brifo.server.global.config

import com.brifo.server.auth.security.SignupTokenCookieManager
import com.brifo.server.global.openapi.ApiDocumentation
import com.brifo.server.global.openapi.ApiErrorCatalog
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {
    @Bean
    fun apiDocumentation(): ApiDocumentation =
        ApiDocumentation(
            operations = ApiErrorCatalog.operations,
            publicOperations = ApiErrorCatalog.publicOperations,
        )

    @Bean
    fun openAPI(): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title("BRIFO BE API")
                    .version("v1")
                    .description("BRIFO backend API documentation"),
            ).components(
                Components()
                    .addSecuritySchemes(
                        "bearerAuth",
                        SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT"),
                    ).addSecuritySchemes(
                        "signupCsrf",
                        SecurityScheme()
                            .type(SecurityScheme.Type.APIKEY)
                            .`in`(SecurityScheme.In.HEADER)
                            .name(SignupTokenCookieManager.CSRF_HEADER_NAME)
                            .description("회원가입 응답 헤더 또는 개발용 응답의 result.csrfToken 값을 입력합니다."),
                    ),
            ).addSecurityItem(SecurityRequirement().addList("bearerAuth"))
}
