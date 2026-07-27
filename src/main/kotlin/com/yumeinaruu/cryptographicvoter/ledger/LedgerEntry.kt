package com.yumeinaruu.cryptographicvoter.ledger

data class LedgerEntry<T>(
    val key: String,
    val value: T,
    val txId: Long,
)
