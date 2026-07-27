package com.yumeinaruu.cryptographicvoter.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "immudb")
data class ImmuDbProperties(
    val host: String = "localhost",
    val port: Int = 3322,
    val user: String = "immudb",
    val password: String = "immudb123!",
    val database: String = "defaultdb",
)
