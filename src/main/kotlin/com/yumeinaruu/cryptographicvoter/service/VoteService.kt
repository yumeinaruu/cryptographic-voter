package com.yumeinaruu.cryptographicvoter.service

import com.yumeinaruu.cryptographicvoter.dto.CastVoteRequest
import com.yumeinaruu.cryptographicvoter.dto.TallyResponse
import com.yumeinaruu.cryptographicvoter.dto.VoteVerificationResponse
import com.yumeinaruu.cryptographicvoter.exception.AlreadyVotedException
import com.yumeinaruu.cryptographicvoter.exception.PollClosedException
import com.yumeinaruu.cryptographicvoter.ledger.LedgerService
import com.yumeinaruu.cryptographicvoter.model.PollStatus
import com.yumeinaruu.cryptographicvoter.model.VoteRecord
import com.yumeinaruu.cryptographicvoter.model.VoteReceipt
import com.yumeinaruu.cryptographicvoter.model.VoterRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
class VoteService(
    private val ledger: LedgerService,
    private val pollService: PollService,
    @Value("\${voting.salt}") private val salt: String,
) {

    private val voterLocks = ConcurrentHashMap<String, Any>()

    fun castVote(pollId: String, req: CastVoteRequest): VoteReceipt {
        val poll = pollService.getPoll(pollId)
        if (poll.status != PollStatus.OPEN) {
            throw PollClosedException(pollId)
        }

        val voterHash = sha256(req.oneTimeCode + salt)
        val voterKey = "voter:$pollId:$voterHash"
        val lock = voterLocks.computeIfAbsent(voterHash) { Any() }

        synchronized(lock) {
            if (ledger.getOrNull(voterKey, VoterRecord::class.java) != null) {
                throw AlreadyVotedException()
            }

            val voteId = UUID.randomUUID().toString()
            val voteKey = "vote:$pollId:$voteId"
            val txId = ledger.put(voteKey, VoteRecord(req.option, Instant.now()))
            ledger.put(voterKey, VoterRecord(voteId))

            return VoteReceipt(voteId, txId, req.option)
        }
    }

    fun verifyVote(pollId: String, voteId: String): VoteVerificationResponse? =
        ledger.getOrNull("vote:$pollId:$voteId", VoteRecord::class.java)?.let { entry ->
            VoteVerificationResponse(
                voteId = voteId,
                option = entry.value.option,
                timestamp = entry.value.timestamp,
                verified = true,
                txId = entry.txId,
            )
        }

    fun tally(pollId: String): TallyResponse {
        val poll = pollService.getPoll(pollId)
        val counts = poll.options.associateWith { 0 }.toMutableMap()

        ledger.scanByPrefix("vote:$pollId:", VoteRecord::class.java).forEach { entry ->
            counts.merge(entry.value.option, 1, Int::plus)
        }

        return TallyResponse(pollId, counts)
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
