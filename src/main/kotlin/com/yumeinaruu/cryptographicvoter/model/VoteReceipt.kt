package com.yumeinaruu.cryptographicvoter.model

data class VoteReceipt(
    val voteId: String,
    val txId: Long,
    val option: String,
)
