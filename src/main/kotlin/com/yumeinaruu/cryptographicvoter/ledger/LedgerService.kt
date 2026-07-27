package com.yumeinaruu.cryptographicvoter.ledger

import io.codenotary.immudb4j.ImmuClient
import io.codenotary.immudb4j.exceptions.KeyNotFoundException
import io.codenotary.immudb4j.exceptions.VerificationException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper


@Service
class LedgerService(
    private val immuClient: ImmuClient,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun <T> put(key: String, value: T): Long {
        val bytes = objectMapper.writeValueAsBytes(value)
        return immuClient.verifiedSet(key, bytes).id
    }

    fun <T> getOrNull(key: String, clazz: Class<T>): LedgerEntry<T>? =
        try {
            val entry = immuClient.verifiedGet(key)
            LedgerEntry(key, objectMapper.readValue(entry.value, clazz), entry.tx)
        } catch (e: KeyNotFoundException) {
            null
        } catch (e: VerificationException) {
            log.error("tamper_detected key={}", key, e)
            throw e
        }

    fun <T> scanByPrefix(prefix: String, clazz: Class<T>): List<LedgerEntry<T>> =
        immuClient.scanAll(prefix).map { entry ->
            LedgerEntry(String(entry.key), objectMapper.readValue(entry.value, clazz), entry.tx)
        }

    fun currentRootHash(): Pair<String, Long> {
        val state = immuClient.currentState()
        return state.txHash.joinToString("") { "%02x".format(it) } to state.txId
    }
}
