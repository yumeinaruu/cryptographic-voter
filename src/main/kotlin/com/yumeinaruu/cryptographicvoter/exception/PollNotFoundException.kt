package com.yumeinaruu.cryptographicvoter.exception

import org.springframework.http.HttpStatus

class PollNotFoundException(pollId: String) : ApiException("Poll $pollId not found", HttpStatus.NOT_FOUND)
