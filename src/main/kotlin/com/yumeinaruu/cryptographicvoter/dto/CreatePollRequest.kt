package com.yumeinaruu.cryptographicvoter.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import java.time.Instant

data class CreatePollRequest(
    @field:NotBlank val question: String,
    @field:NotEmpty val options: List<@NotBlank String>,
    val closesAt: Instant? = null,
    @field:Min(1) val voterCount: Int = 1,
)
