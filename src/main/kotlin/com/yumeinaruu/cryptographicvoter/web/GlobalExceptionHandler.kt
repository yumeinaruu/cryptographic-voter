package com.yumeinaruu.cryptographicvoter.web

import com.yumeinaruu.cryptographicvoter.exception.ApiException
import io.codenotary.immudb4j.exceptions.VerificationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(ApiException::class)
    fun handleApiException(e: ApiException): ResponseEntity<ApiResponse<Nothing>> =
        ApiResponse.error(e.status, e.message ?: "error")


    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Nothing>> =
        ApiResponse.error(
            HttpStatus.BAD_REQUEST,
            e.bindingResult.fieldErrors.joinToString { "${it.field}: ${it.defaultMessage}" },
        )

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(e: Exception): ResponseEntity<ApiResponse<Nothing>> {
        log.error("unhandled_exception", e)
        return ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера")
    }
}
