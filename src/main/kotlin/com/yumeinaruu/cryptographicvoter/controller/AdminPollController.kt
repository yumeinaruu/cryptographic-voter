package com.yumeinaruu.cryptographicvoter.controller

import com.yumeinaruu.cryptographicvoter.dto.CloseResponse
import com.yumeinaruu.cryptographicvoter.dto.CreatePollRequest
import com.yumeinaruu.cryptographicvoter.dto.CreatePollResponse
import com.yumeinaruu.cryptographicvoter.service.PollService
import com.yumeinaruu.cryptographicvoter.web.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/polls")
class AdminPollController(
    private val pollService: PollService,
) {

    @PostMapping
    fun createPoll(@Valid @RequestBody req: CreatePollRequest): ResponseEntity<ApiResponse<CreatePollResponse>> =
        ApiResponse.created(pollService.createPoll(req))

    @PostMapping("/{pollId}/close")
    fun closePoll(@PathVariable pollId: String): ResponseEntity<ApiResponse<CloseResponse>> =
        ApiResponse.ok(pollService.closePoll(pollId))
}
