package com.yumeinaruu.cryptographicvoter.dto

import com.yumeinaruu.cryptographicvoter.model.Poll

data class CreatePollResponse(
    val poll: Poll,
    val oneTimeCodes: List<String>,
)
