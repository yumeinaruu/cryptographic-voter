package com.yumeinaruu.cryptographicvoter.dto

import java.time.Instant

data class VoteVerificationResponse(
    val voteId: String,
    val option: String,
    val timestamp: Instant,
    val verified: Boolean,
    val txId: Long,
)
