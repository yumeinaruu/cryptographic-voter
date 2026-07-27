package com.yumeinaruu.cryptographicvoter.model

import java.time.Instant

data class DigestRecord(
    val rootHash: String,
    val txId: Long,
    val closedAt: Instant,
)
