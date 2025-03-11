package com.tangem.domain.tokens.error

sealed class CurrencyStatusError {

    data object UnableToCreateCurrency : CurrencyStatusError()

    data class DataError(val cause: Throwable) : CurrencyStatusError()
}