package com.brifo.server.global.common

import com.brifo.server.global.code.BaseCode

data class ApiResponse<out T>(
	val success: Boolean,
	val code: String,
	val message: String,
	val result: T? = null,
) {
	companion object {
		fun <T> success(
			code: BaseCode,
			result: T,
		): ApiResponse<T> =
			ApiResponse(
				success = true,
				code = code.code,
				message = code.message,
				result = result,
			)

		fun success(code: BaseCode): ApiResponse<Nothing> =
			ApiResponse(
				success = true,
				code = code.code,
				message = code.message,
			)

		fun error(code: BaseCode): ApiResponse<Nothing> =
			ApiResponse(
				success = false,
				code = code.code,
				message = code.message,
			)

		fun error(
			code: BaseCode,
			message: String,
		): ApiResponse<Nothing> =
			ApiResponse(
				success = false,
				code = code.code,
				message = message,
			)

		fun <T> error(
			code: BaseCode,
			result: T,
		): ApiResponse<T> =
			ApiResponse(
				success = false,
				code = code.code,
				message = code.message,
				result = result,
			)
	}
}
