package com.yumeinaruu.cryptographicvoter.web

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

data class ApiResponse<T>(
    val data: T? = null,
    val error: String? = null,
) {
    companion object {
        fun <T> ok(data: T): ResponseEntity<ApiResponse<T>> = status(HttpStatus.OK, data)

        fun <T> created(data: T): ResponseEntity<ApiResponse<T>> = status(HttpStatus.CREATED, data)

        fun <T> status(status: HttpStatus, data: T): ResponseEntity<ApiResponse<T>> =
            ResponseEntity.status(status).body(ApiResponse(data = data))

        fun <T> error(status: HttpStatus, message: String): ResponseEntity<ApiResponse<T>> =
            ResponseEntity.status(status).body(ApiResponse(error = message))
    }
}
