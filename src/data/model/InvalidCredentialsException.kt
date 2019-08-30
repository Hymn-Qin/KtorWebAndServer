package com.geely.gic.hmi.data.model

class InvalidCredentialsException(
    message: String
) : RuntimeException(message)

class InvalidAccountException(
    message: String
) : RuntimeException(message)