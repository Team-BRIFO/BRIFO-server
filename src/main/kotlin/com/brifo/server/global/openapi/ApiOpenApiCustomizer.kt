package com.brifo.server.global.openapi

import com.brifo.server.auth.code.AuthErrorCode
import com.brifo.server.global.openapi.ApiDocumentationModels.Endpoint
import com.brifo.server.global.openapi.ApiDocumentationModels.ErrorSpec
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.examples.Example
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.responses.ApiResponse
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

@Component
class ApiOpenApiCustomizer(
    private val documentation: ApiDocumentation,
) : OpenApiCustomizer {
    override fun customise(openApi: OpenAPI) {
        registerErrorResponseSchema(openApi)
        validateDocumentedOperations(openApi)
        applyPublicOperationSecurity(openApi)
        applyErrorResponses(openApi)
    }

    private fun registerErrorResponseSchema(openApi: OpenAPI) {
        val errorResponseSchema =
            ObjectSchema()
                .addProperty("success", BooleanSchema().example(false))
                .addProperty("code", StringSchema().example("COMMON_400"))
                .addProperty("message", StringSchema().example("잘못된 요청입니다."))
                .required(listOf("success", "code", "message"))

        val components = openApi.components ?: Components().also { openApi.components = it }
        components.addSchemas(ERROR_RESPONSE_SCHEMA_NAME, errorResponseSchema)
    }

    private fun validateDocumentedOperations(openApi: OpenAPI) {
        val documentedOperations =
            documentation.operations.map { it.endpoint }.toSet() + documentation.publicOperations

        documentedOperations.forEach { key ->
            check(findOperation(openApi, key) != null) {
                "OpenAPI operation does not exist: ${key.method} ${key.path}"
            }
        }
    }

    private fun applyPublicOperationSecurity(openApi: OpenAPI) {
        documentation.publicOperations.forEach { key ->
            findOperation(openApi, key)?.security = emptyList()
        }
    }

    private fun applyErrorResponses(openApi: OpenAPI) {
        documentation.operations.forEach { operationSpec ->
            val operation = checkNotNull(findOperation(openApi, operationSpec.endpoint))
            val applicableErrors =
                if (operationSpec.endpoint in documentation.publicOperations) {
                    operationSpec.errors + ApiErrorSpecs.internalServerError
                } else {
                    operationSpec.errors + authenticationErrors + ApiErrorSpecs.internalServerError
                }
            val errorsByStatus =
                applicableErrors
                    .distinctBy { it.code.code }
                    .groupBy { it.code.status }

            errorsByStatus.forEach { (status, errors) ->
                val response = createErrorResponse(status, errors)

                operation.responses.addApiResponse(
                    status.value().toString(),
                    response,
                )
            }
        }
    }

    private fun createErrorResponse(
        status: HttpStatus,
        errors: List<ErrorSpec>,
    ): ApiResponse {
        val examples =
            errors.associate { error ->
                error.code.code to
                    Example()
                        .summary(error.condition)
                        .value(
                            mapOf(
                                "success" to false,
                                "code" to error.code.code,
                                "message" to error.code.message,
                            ),
                        )
            }
        val mediaType =
            MediaType()
                .schema(Schema<Any>().`$ref`(ERROR_RESPONSE_SCHEMA_REF))
                .examples(examples)

        return ApiResponse()
            .description(status.reasonPhrase)
            .content(Content().addMediaType(APPLICATION_JSON, mediaType))
    }

    private fun findOperation(
        openApi: OpenAPI,
        endpoint: Endpoint,
    ): Operation? =
        openApi.paths
            ?.get(endpoint.path)
            ?.readOperationsMap()
            ?.get(io.swagger.v3.oas.models.PathItem.HttpMethod.valueOf(endpoint.method.name()))

    private companion object {
        const val APPLICATION_JSON = "application/json"
        const val ERROR_RESPONSE_SCHEMA_NAME = "ApiErrorResponse"
        const val ERROR_RESPONSE_SCHEMA_REF = "#/components/schemas/$ERROR_RESPONSE_SCHEMA_NAME"

        val authenticationErrors =
            listOf(
                ErrorSpec(AuthErrorCode.UNAUTHORIZED, "인증 토큰이 전달되지 않은 경우"),
                ErrorSpec(AuthErrorCode.INVALID_TOKEN, "인증 토큰의 형식, 서명, 만료일 또는 타입이 유효하지 않은 경우"),
                ErrorSpec(AuthErrorCode.FORBIDDEN, "인증 토큰에 해당 API를 호출할 권한이 없는 경우"),
            )

    }
}
