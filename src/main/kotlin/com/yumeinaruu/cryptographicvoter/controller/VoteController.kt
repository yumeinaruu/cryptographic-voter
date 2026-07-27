package com.yumeinaruu.cryptographicvoter.controller

import com.yumeinaruu.cryptographicvoter.dto.CastVoteRequest
import com.yumeinaruu.cryptographicvoter.dto.VoteVerificationResponse
import com.yumeinaruu.cryptographicvoter.model.VoteReceipt
import com.yumeinaruu.cryptographicvoter.service.VoteService
import com.yumeinaruu.cryptographicvoter.web.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/polls/{pollId}")
class VoteController(private val voteService: VoteService) {

    @PostMapping("/vote")
    fun vote(
        @PathVariable pollId: String,
        @Valid @RequestBody req: CastVoteRequest,
    ): ResponseEntity<ApiResponse<VoteReceipt>> =
        ApiResponse.created(voteService.castVote(pollId, req))

    @GetMapping("/verify/{voteId}")
    fun verify(
        @PathVariable pollId: String,
        @PathVariable voteId: String,
    ): ResponseEntity<ApiResponse<VoteVerificationResponse>> =
        voteService.verifyVote(pollId, voteId)
            ?.let { ApiResponse.ok(it) }
            ?: ApiResponse.error(HttpStatus.NOT_FOUND, "Голос не найден")
}
