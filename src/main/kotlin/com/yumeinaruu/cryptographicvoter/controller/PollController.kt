package com.yumeinaruu.cryptographicvoter.controller

import com.yumeinaruu.cryptographicvoter.dto.TallyResponse
import com.yumeinaruu.cryptographicvoter.model.Poll
import com.yumeinaruu.cryptographicvoter.service.PollService
import com.yumeinaruu.cryptographicvoter.service.VoteService
import com.yumeinaruu.cryptographicvoter.web.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/polls/{pollId}")
class PollController(
    private val pollService: PollService,
    private val voteService: VoteService,
) {

    @GetMapping
    fun getPoll(@PathVariable pollId: String): ResponseEntity<ApiResponse<Poll>> =
        ApiResponse.ok(pollService.getPoll(pollId))

    @GetMapping("/tally")
    fun tally(@PathVariable pollId: String): ResponseEntity<ApiResponse<TallyResponse>> =
        ApiResponse.ok(voteService.tally(pollId))
}
