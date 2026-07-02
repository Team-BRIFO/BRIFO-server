package com.brifo.server.global.code

import org.springframework.http.HttpStatus

interface BaseCode {
    val status: HttpStatus
    val code: String
    val message: String
}
