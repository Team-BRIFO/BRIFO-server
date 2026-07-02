package com.brifo.server.global.error

data class FieldErrorResponse(
    val field: String,
    val message: String,
)
