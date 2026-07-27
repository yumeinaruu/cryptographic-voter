package com.yumeinaruu.cryptographicvoter.exception

import org.springframework.http.HttpStatus

class PollClosedException(pollId: String) : ApiException("Poll $pollId is closed", HttpStatus.CONFLICT)
