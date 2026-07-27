# Cryptographic Voter

## How it works

immudb is an immutable ledger (append-only, cryptographically linked
structure). every entry joins a hash chain of transactions, and `verifiedGet`
recomputes that chain on the client instead of just trusting the server's
response. If someone tampers with the data on the server's disk,
`verifiedGet` detects the mismatch and throws `VerificationException`
instead of silently returning forged data.

### Key schema

| Key | Value | Purpose |
|---|---|---|
| `poll:{pollId}` | `Poll` | question, options, status |
| `vote:{pollId}:{voteId}` | `VoteRecord` | a single vote |
| `voter:{pollId}:{voterHash}` | `VoterRecord` | anti-duplicate: code already used |
| `digest:{pollId}:closed` | `DigestRecord` | ledger root hash at the moment the poll was closed |

`voterHash = SHA256(oneTimeCode + salt)` a vote is anonymous, but voting
twice with the same code is impossible. `voteId` is a `UUID.randomUUID()`,
deliberately unpredictable. a predictable id would let someone brute-force
`/verify/{voteId}` and deanonymize other people's votes.
