package com.brifo.server.global.openapi

import com.brifo.server.global.openapi.ApiDocumentationModels.Endpoint
import com.brifo.server.global.openapi.ApiDocumentationModels.OperationSpec

data class ApiDocumentation(
    val operations: List<OperationSpec>,
    val publicOperations: Set<Endpoint>,
) {
    init {
        val duplicatedOperations =
            operations
                .groupingBy(OperationSpec::endpoint)
                .eachCount()
                .filterValues { it > 1 }
                .keys
        require(duplicatedOperations.isEmpty()) {
            "Duplicated OpenAPI operation documentation: $duplicatedOperations"
        }

        operations.forEach { documentation ->
            val duplicatedCodes =
                documentation.errors
                    .groupingBy { it.code.code }
                    .eachCount()
                    .filterValues { it > 1 }
                    .keys
            require(duplicatedCodes.isEmpty()) {
                "Duplicated error codes for ${documentation.endpoint}: $duplicatedCodes"
            }
        }
    }
}
