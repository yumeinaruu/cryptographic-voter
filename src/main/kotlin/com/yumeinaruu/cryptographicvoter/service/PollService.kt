package com.yumeinaruu.cryptographicvoter.service

import com.yumeinaruu.cryptographicvoter.dto.CloseResponse
import com.yumeinaruu.cryptographicvoter.dto.CreatePollRequest
import com.yumeinaruu.cryptographicvoter.dto.CreatePollResponse
import com.yumeinaruu.cryptographicvoter.exception.PollClosedException
import com.yumeinaruu.cryptographicvoter.exception.PollNotFoundException
import com.yumeinaruu.cryptographicvoter.ledger.LedgerService
import com.yumeinaruu.cryptographicvoter.model.DigestRecord
import com.yumeinaruu.cryptographicvoter.model.Poll
import com.yumeinaruu.cryptographicvoter.model.PollStatus
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class PollService(
    private val ledger: LedgerService,
) {

    fun createPoll(req: CreatePollRequest): CreatePollResponse {
        val pollId = UUID.randomUUID().toString()
        val poll = Poll(
            id = pollId,
            question = req.question,
            options = req.options,
            createdAt = Instant.now(),
            closesAt = req.closesAt,
            status = PollStatus.OPEN,
        )
        ledger.put(pollKey(pollId), poll)


        val oneTimeCodes = List(req.voterCount) { UUID.randomUUID().toString() }

        return CreatePollResponse(poll, oneTimeCodes)
    }

    fun getPoll(pollId: String): Poll =
        ledger.getOrNull(pollKey(pollId), Poll::class.java)?.value
            ?: throw PollNotFoundException(pollId)

    fun closePoll(pollId: String): CloseResponse {
        val poll = getPoll(pollId)
        if (poll.status == PollStatus.CLOSED) {
            throw PollClosedException(pollId)
        }

        ledger.put(pollKey(pollId), poll.copy(status = PollStatus.CLOSED))

        val (rootHash, txId) = ledger.currentRootHash()
        ledger.put(
            "digest:$pollId:closed",
            DigestRecord(rootHash = rootHash, txId = txId, closedAt = Instant.now()),
        )

        return CloseResponse(rootHash, txId)
    }

    private fun pollKey(pollId: String) = "poll:$pollId"
}
