package com.yumeinaruu.cryptographicvoter.dto

import jakarta.validation.constraints.NotBlank

data class CastVoteRequest(
    @field:NotBlank val option: String,
    @field:NotBlank val oneTimeCode: String,
)
