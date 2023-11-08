package com.tangem.domain.tokens.error

sealed class CurrencyStatusError {

    object UnableToCreateCurrency : CurrencyStatusError()

    data class DataError(val cause: Throwable) : CurrencyStatusError()
}