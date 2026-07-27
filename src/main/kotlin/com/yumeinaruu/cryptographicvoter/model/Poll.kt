package com.yumeinaruu.cryptographicvoter.model

import java.time.Instant

data class Poll(
    val id: String,
    val question: String,
    val options: List<String>,
    val createdAt: Instant,
    val closesAt: Instant?,
    val status: PollStatus,
)
