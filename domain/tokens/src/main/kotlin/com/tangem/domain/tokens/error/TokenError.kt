package com.tangem.domain.tokens.error

sealed class TokenError {

    object UnableToCreateToken : TokenError()

    data class DataError(val cause: Throwable) : TokenError()
}