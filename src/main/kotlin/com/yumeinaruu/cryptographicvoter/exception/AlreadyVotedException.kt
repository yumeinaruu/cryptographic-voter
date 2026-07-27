package com.yumeinaruu.cryptographicvoter.exception

import org.springframework.http.HttpStatus

class AlreadyVotedException : ApiException("Code was already used", HttpStatus.CONFLICT)
