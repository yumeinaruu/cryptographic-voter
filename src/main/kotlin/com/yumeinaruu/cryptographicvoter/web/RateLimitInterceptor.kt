package com.yumeinaruu.cryptographicvoter.web

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import tools.jackson.databind.ObjectMapper
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Component
class RateLimitInterceptor(private val objectMapper: ObjectMapper) : HandlerInterceptor {

    private val maxRequests = 10
    private val window: Duration = Duration.ofMinutes(1)
    private val requestTimestamps = ConcurrentHashMap<String, MutableList<Instant>>()

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val clientIp = request.remoteAddr
        val now = Instant.now()
        val windowStart = now.minus(window)

        val timestamps = requestTimestamps.computeIfAbsent(clientIp) { mutableListOf() }
        synchronized(timestamps) {
            timestamps.removeIf { it.isBefore(windowStart) }
            if (timestamps.size >= maxRequests) {
                response.status = HttpStatus.TOO_MANY_REQUESTS.value()
                response.contentType = "application/json"
                response.writer.write(
                    objectMapper.writeValueAsString(ApiResponse<Unit>(error = "Слишком много попыток, попробуйте позже")),
                )
                return false
            }
            timestamps.add(now)
        }
        return true
    }
}
