package com.yumeinaruu.cryptographicvoter.model

import java.time.Instant

data class VoteRecord(
    val option: String,
    val timestamp: Instant,
)
