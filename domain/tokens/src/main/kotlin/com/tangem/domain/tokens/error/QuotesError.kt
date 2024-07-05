package com.tangem.domain.tokens.error

sealed class QuotesError {
    data class DataError(val cause: Throwable) : QuotesError()
}