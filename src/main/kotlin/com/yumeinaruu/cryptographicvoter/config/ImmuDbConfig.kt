package com.yumeinaruu.cryptographicvoter.config

import io.codenotary.immudb4j.ImmuClient
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(ImmuDbProperties::class)
class ImmuDbConfig(
    private val properties: ImmuDbProperties,
) {

    @Bean(destroyMethod = "shutdown")
    fun immuClient(): ImmuClient {
        val client = ImmuClient.newBuilder()
            .withServerUrl(properties.host)
            .withServerPort(properties.port)
            .build()
        client.openSession(properties.database, properties.user, properties.password)
        return client
    }

}
