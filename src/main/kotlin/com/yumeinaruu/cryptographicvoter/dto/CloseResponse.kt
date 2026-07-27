package com.yumeinaruu.cryptographicvoter.dto

data class CloseResponse(
    val rootHash: String,
    val txId: Long,
)
