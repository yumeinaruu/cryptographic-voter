package com.yumeinaruu.cryptographicvoter.exception

import org.springframework.http.HttpStatus


abstract class ApiException(
    message: String,
    val status: HttpStatus,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
