package com.tangem.domain.tokens.error

sealed class GetCurrenciesError {

    data class DataError(val cause: Throwable) : GetCurrenciesError()
}