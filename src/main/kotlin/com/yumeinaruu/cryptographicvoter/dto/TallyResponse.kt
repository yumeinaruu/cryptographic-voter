package com.yumeinaruu.cryptographicvoter.dto

data class TallyResponse(
    val pollId: String,
    val counts: Map<String, Int>,
)
