package com.brifo.server.global.openapi

import com.brifo.server.global.code.BaseCode
import org.springframework.http.HttpMethod

object ApiDocumentationModels {
    data class Endpoint(
        val method: HttpMethod,
        val path: String,
    )

    data class ErrorSpec(
        val code: BaseCode,
        val condition: String,
    )

    data class OperationSpec(
        val endpoint: Endpoint,
        val errors: List<ErrorSpec>,
    ) {
        constructor(
            method: HttpMethod,
            path: String,
            vararg errors: ErrorSpec,
        ) : this(Endpoint(method, path), errors.toList())
    }
}
