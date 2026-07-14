package com.brifo.server.global.error

import com.brifo.server.global.code.ErrorCode
import com.brifo.server.global.common.ApiResponse
import com.brifo.server.global.exception.BusinessException
import jakarta.validation.ConstraintViolationException
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.BindException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class GlobalExceptionHandler(
    private val environment: Environment,
) {
    private val suppressErrorDetails: Boolean
        get() = environment.acceptsProfiles(Profiles.of("prod"))

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(exception: BusinessException): ResponseEntity<ApiResponse<Nothing>> {
        val errorCode = exception.errorCode
        val message = if (suppressErrorDetails) errorCode.message else exception.message

        return ResponseEntity
            .status(errorCode.status)
            .body(ApiResponse.error(errorCode, message))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(exception: MethodArgumentNotValidException): ResponseEntity<ApiResponse<*>> {
        if (suppressErrorDetails) {
            return ResponseEntity
                .status(ErrorCode.INVALID_REQUEST.status)
                .body(ApiResponse.error(ErrorCode.INVALID_REQUEST))
        }

        val errors =
            exception.bindingResult.fieldErrors.map {
                FieldErrorResponse(
                    field = it.field,
                    message = it.defaultMessage ?: ErrorCode.INVALID_REQUEST.message,
                )
            }

        return ResponseEntity
            .status(ErrorCode.INVALID_REQUEST.status)
            .body(ApiResponse.error(ErrorCode.INVALID_REQUEST, errors))
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(exception: ConstraintViolationException): ResponseEntity<ApiResponse<*>> {
        if (suppressErrorDetails) {
            return ResponseEntity
                .status(ErrorCode.INVALID_REQUEST.status)
                .body(ApiResponse.error(ErrorCode.INVALID_REQUEST))
        }

        val errors =
            exception.constraintViolations.map {
                FieldErrorResponse(
                    field = it.propertyPath.toString(),
                    message = it.message,
                )
            }

        return ResponseEntity
            .status(ErrorCode.INVALID_REQUEST.status)
            .body(ApiResponse.error(ErrorCode.INVALID_REQUEST, errors))
    }

    @ExceptionHandler(
        MethodArgumentTypeMismatchException::class,
        HttpMessageNotReadableException::class,
        MissingServletRequestParameterException::class,
        BindException::class,
    )
    fun handleInvalidRequestException(exception: Exception): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity
            .status(ErrorCode.INVALID_REQUEST.status)
            .body(ApiResponse.error(ErrorCode.INVALID_REQUEST))

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleHttpRequestMethodNotSupportedException(
        exception: HttpRequestMethodNotSupportedException,
    ): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity
            .status(ErrorCode.METHOD_NOT_ALLOWED.status)
            .body(ApiResponse.error(ErrorCode.METHOD_NOT_ALLOWED))

    @ExceptionHandler(Exception::class)
    fun handleException(exception: Exception): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity
            .status(ErrorCode.INTERNAL_SERVER_ERROR.status)
            .body(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR))
}
