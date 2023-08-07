package com.tangem.domain.tokens.error

sealed class CurrencyError {

    object UnableToCreateCurrency : CurrencyError()

    data class DataError(val cause: Throwable) : CurrencyError()
}